package cat.indiketa.degiro.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;

/**
 *
 * @author indiketa
 */
@Data
public class DOrderConfirmation implements IValidable {
    // BUY: {"data":{"confirmationId":"11caa4dd-c1f2-4c0a-b1c2-e6f21c04a4be","transactionFee":0.50,"showExAnteReportLink":true}}
    // SELL: {"data":{"confirmationId":"81773437-bccd-4a65-a651-d711f9d956ce","transactionOppositeFee":0.50,"showExAnteReportLink":true}})
    private String confirmationId;
    @SerializedName(value="transactionFee", alternate={"transactionOppositeFee"})
    private Double transactionFee;
    private Boolean showExAnteReportLink;

    @Override
    public boolean isInvalid() {
        return confirmationId == null || transactionFee == null || showExAnteReportLink == null;
    }
}
