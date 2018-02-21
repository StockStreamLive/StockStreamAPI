package service.dao;

import com.google.common.collect.ImmutableList;
import org.springframework.beans.factory.annotation.Autowired;
import service.dao.cache.WalletCache;
import stockstream.database.Wallet;
import stockstream.database.WalletRegistry;
import stockstream.util.RandomUtil;

import java.util.List;
import java.util.Optional;

public class WalletDao {

    @Autowired
    private WalletRegistry walletRegistry;

    @Autowired
    private WalletCache walletCache;

    public List<String> getPlayers() {
        return walletCache.getPlayersByRealizedReturn();
    }

    public Wallet getPlayerWallet(final String player) {
        return walletCache.getPlayerToWallet().get(player);
    }

    public String getNextReferralCode() {
        final Optional<String> winner = RandomUtil.randomChoice(walletCache.getReferralToWallet().keySet());
        if (!winner.isPresent()) {
            return "https://robinhood.com";
        }
        final String referralCode = winner.get();
        final Wallet winnerWallet = walletCache.getReferralToWallet().get(referralCode);

        winnerWallet.setReferralClicks(winnerWallet.getReferralClicks() + 1);
        walletRegistry.updateWallets(ImmutableList.of(winnerWallet));

        return referralCode;
    }

}
