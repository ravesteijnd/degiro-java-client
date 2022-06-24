package cat.indiketa.degiro.model;

import lombok.Data;

import java.util.List;

/**
 *
 * @author indiketa
 */
@Data
public class DOrderConfirmation implements IValidable {
    //"{"data":{"confirmationId":"11caa4dd-c1f2-4c0a-b1c2-e6f21c04a4be","transactionFee":0.50,"showExAnteReportLink":true}}"

    private String confirmationId;
    private Double transactionFee;
    private Boolean showExAnteReportLink;

    @Override
    public boolean isInvalid() {
        return confirmationId == null || transactionFee == null || showExAnteReportLink == null;
    }
}
