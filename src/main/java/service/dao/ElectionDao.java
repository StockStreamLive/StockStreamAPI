package service.dao;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableSet;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import service.application.Config;
import service.application.LiveCommands;
import service.data.request.RequestAuth;
import service.data.request.RequestUser;
import service.data.request.SpeedVoteRequest;
import service.data.request.TradeVoteRequest;
import stockstream.computer.OrderComputer;
import stockstream.data.OrderStatus;
import stockstream.data.Voter;
import stockstream.database.ElectionRegistry;
import stockstream.database.ElectionStub;
import stockstream.database.ElectionVoteStub;
import stockstream.util.JSONUtil;

import javax.xml.bind.DatatypeConverter;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ElectionDao {

    @Autowired
    private ElectionRegistry electionRegistry;

    @Autowired
    private OrderComputer orderComputer;

    @Autowired
    private LiveCommands liveCommands;

    private void augmentElectionStub(final ElectionStub electionStub) throws ExecutionException {
        final Map<String, Integer> polls = new HashMap<>();

        final Collection<ElectionVoteStub> electionVotes = votesCache.get(electionStub.getElectionId());
        electionVotes.forEach(vote -> {
            final Integer existingVotes = polls.computeIfAbsent(vote.getVoteObject(), i -> (0));
            polls.put(vote.getVoteObject(), existingVotes + 1);
        });

        electionStub.setPolls(polls);
    }

    private LoadingCache<String, Collection<ElectionStub>> electionCache =
            CacheBuilder.newBuilder()
                        .expireAfterWrite(500, TimeUnit.MILLISECONDS)
                        .build(new CacheLoader<String, Collection<ElectionStub>>() {
                            @Override
                            public Collection<ElectionStub> load(final String key) throws Exception {
                                final Collection<ElectionStub> elections = electionRegistry.getElections();

                                elections.forEach(electionStub -> {
                                    try {
                                        augmentElectionStub(electionStub);
                                    } catch (ExecutionException e) {
                                        log.warn("Exception augmenting election {} -> {}", electionStub, e.getMessage(), e);
                                    }
                                });

                                return elections;
                            }
                        });

    private LoadingCache<String, Collection<ElectionVoteStub>> votesCache =
            CacheBuilder.newBuilder()
                        .expireAfterWrite(500, TimeUnit.MILLISECONDS)
                        .build(new CacheLoader<String, Collection<ElectionVoteStub>>() {
                            @Override
                            public Collection<ElectionVoteStub> load(final String electionId) throws Exception {
                                return electionRegistry.getElectionVotes(electionId);
                            }
                        });

    public Collection<ElectionStub> getElections() {
        try {
            return electionCache.get("elections");
        } catch (final ExecutionException e) {
            log.warn(e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    public Map<String, Collection<ElectionVoteStub>> getElectionVotes(final Collection<ElectionStub> electionStubs) {
        final Map<String, Collection<ElectionVoteStub>> electionVotes = new HashMap<>();

        electionStubs.forEach(electionStub -> {
            try {
                final String electionId = String.format("%s:%s", electionStub.getTopic(), electionStub.getExpirationDate());
                electionVotes.put(electionStub.getTopic(), votesCache.get(electionId));
            } catch (final ExecutionException e) {
                log.warn(e.getMessage(), e);
            }
        });

        return electionVotes;
    }

    private OrderStatus verifyJWTToken(final RequestAuth requestAuth) {
        try {
            Jwts.parser()
                .setSigningKey(DatatypeConverter.parseBase64Binary(Config.TWITCH_EXTENSION_KEY))
                .parseClaimsJws(requestAuth.getToken())
                .getBody();
        } catch (final SignatureException sigEx) {
            return OrderStatus.UNKNOWN;
        }

        return OrderStatus.OK;
    }

    public OrderStatus processTradeCommandVote(final String serializedRequest) {
        final Optional<TradeVoteRequest> voteRequestOptional = JSONUtil.deserializeObject(serializedRequest, TradeVoteRequest.class);

        if (!voteRequestOptional.isPresent()) {
            return OrderStatus.UNKNOWN;
        }

        final TradeVoteRequest tradeVoteRequest = voteRequestOptional.get();

        OrderStatus orderStatus = verifyJWTToken(tradeVoteRequest.getUser().getAuth());

        if (!OrderStatus.OK.equals(orderStatus)) {
            return orderStatus;
        }

        final RequestUser requestUser = tradeVoteRequest.getUser();

        final Voter voter = new Voter(requestUser.getUsername(), requestUser.getPlatform(), "#stockstream", false);

        try {
            orderStatus = orderComputer.preProcessTradeCommand(tradeVoteRequest.getVote(), ImmutableSet.of(voter));
        } catch (final Exception e) {
            log.warn(e.getMessage(), e);
            orderStatus = OrderStatus.SERVER_EXCEPTION;
        }

        if (OrderStatus.OK.equals(orderStatus)) {
            archiveVoteData(tradeVoteRequest.getElectionId(), tradeVoteRequest.getUser(), tradeVoteRequest.getVote());
            liveCommands.publishCommand(tradeVoteRequest);
        }

        return orderStatus;
    }

    public OrderStatus processSpeedCommandVote(final String serializedRequest) {
        final Optional<SpeedVoteRequest> voteRequestOptional = JSONUtil.deserializeObject(serializedRequest, SpeedVoteRequest.class);

        if (!voteRequestOptional.isPresent()) {
            return OrderStatus.UNKNOWN;
        }

        final SpeedVoteRequest speedVoteRequest = voteRequestOptional.get();

        OrderStatus orderStatus = verifyJWTToken(speedVoteRequest.getUser().getAuth());

        if (!OrderStatus.OK.equals(orderStatus)) {
            return orderStatus;
        }

        if (OrderStatus.OK.equals(orderStatus)) {
            archiveVoteData(speedVoteRequest.getElectionId(), speedVoteRequest.getUser(), speedVoteRequest.getVote());
        }

        return orderStatus;
    }


    private void archiveVoteData(final String electionId, final RequestUser userObject, final Object voteObject) {
        final Voter voter = new Voter(userObject.getUsername(), userObject.getPlatform(), "#stockstream", false);
        final ElectionVoteStub electionVoteStub = new ElectionVoteStub(voter, JSONUtil.serializeObject(voteObject).get(), electionId);

        electionRegistry.saveElectionVotes(ImmutableSet.of(electionVoteStub));
    }
}
