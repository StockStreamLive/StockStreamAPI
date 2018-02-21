package service.application;

import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import service.computer.AutoCompleteEngine;
import service.computer.PositionAssembler;
import service.dao.*;
import service.dao.cache.CacheReloader;
import service.data.Position;
import service.data.RegistrationStatus;
import spark.Spark;
import stockstream.database.PlayerVote;
import stockstream.database.Wallet;
import stockstream.util.JSONUtil;

import java.util.*;

import static spark.Spark.*;

@Slf4j
public class APIService {

    @Autowired
    private ScoreDao scoreDao;

    @Autowired
    private OrdersDao ordersDao;

    @Autowired
    private VotesDao votesDao;

    @Autowired
    private WalletDao walletDao;

    @Autowired
    private PortfolioDao portfolioDao;

    @Autowired
    private PositionsDao positionsDao;

    @Autowired
    private ElectionDao electionDao;

    @Autowired
    private GameStateDao gameStateDao;

    @Autowired
    private ContestDao contestDao;

    @Autowired
    private PositionAssembler positionAssembler;

    @Autowired
    private AutoCompleteEngine autoCompleteEngine;

    @Autowired
    private CacheReloader cacheReloader;

    @Autowired
    private LiveCommands liveCommands;

    private void enableCORS() {
        options("/*", (request, response) -> {

            String accessControlRequestHeaders = request.headers("Access-Control-Request-Headers");
            if (accessControlRequestHeaders != null) {
                response.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
            }

            String accessControlRequestMethod = request.headers("Access-Control-Request-Method");
            if (accessControlRequestMethod != null) {
                response.header("Access-Control-Allow-Methods", accessControlRequestMethod);
            }

            return "OK";
        });

        before((request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Request-Method", "GET, POST, PUT, DELETE, OPTIONS");
            response.header("Access-Control-Allow-Headers", "Content-Type,Authorization,X-Requested-With,Content-Length,Accept,Origin,");
            response.type("application/json");
        });
    }

    public void startServer(final int port) {
        port(port);

        if (Stage.TEST.equals(Config.STAGE)) {
            Spark.secure("keystore.jks", "password", null, null);
        }

        Spark.webSocket("/v1/commands", this.liveCommands);
        Spark.staticFileLocation("/public");
        enableCORS();

        before((request, response) -> response.type("application/json"));

        //========== Misc APIS ==========//

        Spark.get("/", (request, response) -> "{\"documentation\":\"https://stockstream.live/info/api\"}");

        Spark.get("/primed", (request, response) -> cacheReloader.cachesPrimed());

        //========== Portfolio APIS ==========//

        Spark.get("/v1/portfolio/current", (request, response) -> portfolioDao.getCurrentPortfolio());

        Spark.get("/v1/portfolio/date/:date", (request, response) -> portfolioDao.getSnapshotsForDate(request.params(":date")));

        Spark.get("/v1/portfolio/date/:date/:filename", (request, response) -> {
            final String dateStr = request.params(":date");
            final String filename = request.params(":filename");
            return portfolioDao.getSnapshotFileForDate(dateStr, filename);
        });

        Spark.get("/v1/portfolio/values", (request, response) -> JSONUtil.serializeObject(portfolioDao.getPortfolioValues()).orElse("[]"));

        Spark.get("/v1/portfolio/values/date/:date", (request, response) -> JSONUtil.serializeObject(portfolioDao.getPortfolioValuesForDate(request.params(":date"))).orElse("{}"));

        //========== Order APIS ==========//

        Spark.get("/v1/orders/date/:date", (request, response) -> JSONUtil.serializeObject(ordersDao.getOrdersForDate(request.params(":date"))).orElse("{}"));

        Spark.get("/v1/orders/symbol/:symbol", (request, response) -> JSONUtil.serializeObject(ordersDao.getOrdersForSymbol(request.params(":symbol"))).orElse("{}"));

        Spark.get("/v1/orders", (request, response) -> JSONUtil.serializeObject(ordersDao.getOrdersForIds(new ArrayList<>(Arrays.asList(request.queryParams("ids").split(","))))).orElse("{}"));

        Spark.get("/v1/orders/stats", (request, response) -> JSONUtil.serializeObject(ordersDao.getOrderStats()).orElse("{}"));

        //========== Vote APIS ==========//

        Spark.get("/v1/votes/date/:date", (request, response) -> JSONUtil.serializeObject(votesDao.getVotesForDate(request.params(":date").replace("-", "/"))).orElse("{}"));

        Spark.get("/v1/votes/player/:player", (request, response) -> JSONUtil.serializeObject(votesDao.getVotesForPlayer(request.params(":player"))).orElse("{}"));

        Spark.get("/v1/votes/order/:orderId", (request, response) -> JSONUtil.serializeObject(votesDao.getVotesForOrderId(request.params(":orderId"))).orElse("{}"));

        Spark.get("/v1/votes/orders", (request, response) -> {
            final String[] ids = request.queryParams("ids").split(",");
            final Map<String, List<PlayerVote>> orderIdToVotes = new HashMap<>();
            for (final String id : ids) {
                orderIdToVotes.put(id, votesDao.getVotesForOrderId(id));
            }
            return JSONUtil.serializeObject(orderIdToVotes).orElse("{}");
        });

        //========== Player APIs ==========//

        Spark.get("/v1/positions/player/:player", (request, response) -> {
            final String player = request.params(":player");
            final Collection<Position> positions = positionsDao.getPositionsForPlayer(player);

            return JSONUtil.serializeObject(positions).orElse("[]");
        });

        Spark.get("/v1/positions/symbol/:symbol", (request, response) -> {
            final String symbol = request.params(":symbol");
            final Collection<Position> positions = positionsDao.getPositionsForSymbol(symbol);

            return JSONUtil.serializeObject(positions).orElse("[]");
        });

        Spark.get("/v1/positions/date/:date", (request, response) -> {
            final String date = request.params(":date");
            final Collection<Position> positions = positionsDao.getPositionsForDate(date);

            return JSONUtil.serializeObject(positions).orElse("[]");
        });

        Spark.get("/v1/players", (request, response) -> JSONUtil.serializeObject(walletDao.getPlayers()).orElse("[]"));

        Spark.get("/v1/wallets/player/:player", (request, response) -> {
            final String player = request.params(":player");
            final Wallet wallet = walletDao.getPlayerWallet(player);

            return JSONUtil.serializeObject(wallet).orElse("[]");
        });

        Spark.get("/v1/referral", (request, response) -> JSONUtil.serializeObject(walletDao.getNextReferralCode()).orElse("[]"));

        Spark.get("/v1/scores", (request, response) -> JSONUtil.serializeObject(scoreDao.getHighScoreList()).orElse("[]"));

        //========== Search APIs ==========//

        Spark.get("/v1/search", (request, response) -> {
            final String query = request.queryParams("term");
            final Collection<AutoCompleteItem> results = autoCompleteEngine.findItems(query);
            return JSONUtil.serializeObject(results).orElse("[]");
        });

        constructGameRoutes();
        constructVoteRoutes();

        Spark.init();
    }


    //========== Vote APIs ==========//
    private void constructVoteRoutes() {

        Spark.get("/v1/elections/votes", (request, response) -> JSONUtil.serializeObject(electionDao.getElectionVotes(electionDao.getElections())).orElse("{}"));

        Spark.post("/v1/vote/trading", (request, response) -> JSONUtil.serializeObject(ImmutableMap.of("status", electionDao.processTradeCommandVote(request.body()))).orElse("{}"));

        Spark.post("/v1/vote/speed", (request, response) -> JSONUtil.serializeObject(ImmutableMap.of("status", electionDao.processSpeedCommandVote(request.body()))).orElse("{}"));

    }

    //========== Game APIs ==========//
    private void constructGameRoutes() {

        Spark.get("/v1/positions/open", (request, response) -> JSONUtil.serializeObject(positionsDao.getOpenPositions()).orElse("[]"));

        Spark.get("/v1/account", (request, response) -> JSONUtil.serializeObject(gameStateDao.getRobinhoodAccountState().get()).orElse("{}"));

        Spark.get("/v1/elections", (request, response) -> JSONUtil.serializeObject(electionDao.getElections()).orElse("[]"));

        Spark.get("/v1/game/state", (request, response) -> JSONUtil.serializeObject(gameStateDao.getGameState().get()).orElse("{}"));

        Spark.get("/v1/registered/:player", (request, response) -> {
            final boolean registered = contestDao.isPlayerRegistered(request.params(":player"));
            return JSONUtil.serializeObject(ImmutableMap.of("registered", registered)).orElse("{}");
        });

        Spark.post("/v1/register", (request, response) -> {
            final RegistrationStatus registrationStatus = contestDao.processRegistration(new JSONObject(request.body()));
            return JSONUtil.serializeObject(ImmutableMap.of("status", registrationStatus)).orElse("{}");
        });
    }

}
