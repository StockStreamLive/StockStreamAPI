package service.dao.cache;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import service.computer.WalletAssembler;
import spark.utils.CollectionUtils;
import stockstream.database.Wallet;
import stockstream.database.WalletOrder;
import stockstream.database.WalletOrderRegistry;
import stockstream.database.WalletRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class WalletCache implements ReloadingCache {

    @Autowired
    private WalletRegistry walletRegistry;

    @Autowired
    private WalletOrderRegistry walletOrderRegistry;

    @Autowired
    private WalletAssembler walletAssembler;

    @Getter
    private Map<String, Wallet> playerToWallet = new ConcurrentHashMap<>();

    @Getter
    private Map<String, List<WalletOrder>> playerToWalletOrders = new ConcurrentHashMap<>();

    @Getter
    private Map<String, WalletOrder> orderIdToWalletOrder = new ConcurrentHashMap<>();

    @Getter
    private Map<String, String> orderIdToPlayer = new ConcurrentHashMap<>();

    @Getter
    private List<WalletOrder> sortedWalletOrders = new ArrayList<>();

    @Getter
    private List<String> playersByRealizedReturn = new ArrayList<>();

    @Getter
    private Map<String, Wallet> referralToWallet = new ConcurrentHashMap<>();

    @Override
    public void reloadCache() {
        final List<WalletOrder> allWalletOrders = walletOrderRegistry.getAllWalletOrders();
        final List<Wallet> allWallets = walletRegistry.getAllPlayerWallets();

        loadWallets(allWallets);
        loadWalletOrders(allWalletOrders);

        this.playersByRealizedReturn = walletAssembler.computeRankedPlayers(this.playerToWallet);
    }

    public void updateWallets(final Map<String, Wallet> newPlayerToWallet) {
        final List<Wallet> changedWallets = new ArrayList<>();
        newPlayerToWallet.forEach((platform_username, newWallet) -> {
            if (!playerToWallet.containsKey(platform_username)) {
                changedWallets.add(newWallet);
                return;
            }
            final Wallet existingWallet = playerToWallet.get(platform_username);
            final boolean walletChanged = existingWallet.indirectValuesChanged(newWallet);
            if (walletChanged) {
                existingWallet.setUnrealizedDollarsSpent(newWallet.getUnrealizedDollarsSpent());
                existingWallet.setRealizedDecimalReturn(newWallet.getRealizedDecimalReturn());
                existingWallet.setRealizedReturn(newWallet.getRealizedReturn());
                changedWallets.add(existingWallet);
            }
        });

        walletRegistry.updateWallets(changedWallets);
    }

    private void loadWallets(final List<Wallet> allWallets) {
        final Map<String, Wallet> playerToWallet = new ConcurrentHashMap<>();

        allWallets.forEach(wallet -> {
            playerToWallet.put(wallet.getPlatform_username(), wallet);
            if (!StringUtils.isEmpty(wallet.getReferralCode())) {
                referralToWallet.put(wallet.getReferralCode(), wallet);
            }
        });

        this.playerToWallet = playerToWallet;
    }

    private void loadWalletOrders(final List<WalletOrder> allWalletOrders) {
        allWalletOrders.sort((o1, o2) -> {
            final String order1Timestamp = o1.getCreated_at();
            final String order2Timestamp = o2.getCreated_at();
            return order1Timestamp.compareTo(order2Timestamp);
        });

        final Map<String, List<WalletOrder>> playerStrToOrders = new ConcurrentHashMap<>();
        final Map<String, WalletOrder> idStrToOrders = new ConcurrentHashMap<>();
        final Map<String, String> idStrToStrPlayer = new ConcurrentHashMap<>();

        allWalletOrders.forEach(order -> {
            playerStrToOrders.computeIfAbsent(order.getPlatform_username(), list -> new ArrayList<>()).add(order);
            idStrToOrders.put(order.getId(), order);
            idStrToStrPlayer.put(order.getId(), order.getPlatform_username());
        });

        log.info("Found {} total walletOrders", idStrToOrders.size());

        this.playerToWalletOrders = playerStrToOrders;
        this.orderIdToWalletOrder = idStrToOrders;
        this.orderIdToPlayer = idStrToStrPlayer;
        this.sortedWalletOrders = allWalletOrders;
    }

    @Override
    public boolean isPrimed() {
        return !CollectionUtils.isEmpty(this.sortedWalletOrders);
    }
}
