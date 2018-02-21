package service.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderStats {

    private int totalBoughtShares = 0;
    private int totalSoldShares = 0;

    private double totalPurchasedAmount = 0;
    private double totalSoldAmount = 0;

}
