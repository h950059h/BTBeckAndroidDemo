package com.braintreepayments.demo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import com.braintreepayments.api.Braintree;
import com.braintreepayments.api.Braintree.PaymentMethodNonceListener;
import com.braintreepayments.api.dropin.BraintreePaymentActivity;
import com.braintreepayments.api.dropin.view.PaymentButton;
import com.braintreepayments.api.models.CardBuilder;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONException;
import org.json.JSONObject;

public class CustomFormActivity extends Activity implements PaymentMethodNonceListener {

    private Braintree mBraintree;
    private PaymentButton mPaymentButton;
    private EditText mCardNumber;
    private EditText mExpirationDate;

    protected void onCreate(Bundle onSaveInstanceState) {
        super.onCreate(onSaveInstanceState);
        setContentView(R.layout.custom);

        //mPaymentButton = (PaymentButton) findViewById(R.id.payment_button);
        mCardNumber = (EditText) findViewById(R.id.card_number);
        mExpirationDate = (EditText) findViewById(R.id.card_expiration_date);

        mBraintree = Braintree.getInstance(this,
                getIntent().getStringExtra(BraintreePaymentActivity.EXTRA_CLIENT_TOKEN));
        mBraintree.addListener(this);
        //mPaymentButton.initialize(this, mBraintree);
    }

    public void onPurchase(View v) {
        CardBuilder cardBuilder = new CardBuilder()
            .cardNumber(mCardNumber.getText().toString())
            .expirationDate(mExpirationDate.getText().toString());

        mBraintree.tokenize(cardBuilder);
    }

    @Override
    public void onPaymentMethodNonce(String paymentMethodNonce) {
        sendNonceToServer(BraintreePaymentActivity.EXTRA_PAYMENT_METHOD_NONCE);
        /*
        setResult(RESULT_OK, new Intent()
                .putExtra(BraintreePaymentActivity.EXTRA_PAYMENT_METHOD_NONCE, paymentMethodNonce));*/
        //finish();
    }

    @Override
    public void onActivityResult(int requestCode, int responseCode, Intent data) {
        showDialog("onActivityResult: ");
        if (requestCode == PaymentButton.REQUEST_CODE) {
            if (responseCode == RESULT_OK) {
                Intent intent = new Intent(this, FinishedActivity.class)
                        .putExtra(BraintreePaymentActivity.EXTRA_PAYMENT_METHOD_NONCE,
                                data.getStringExtra(BraintreePaymentActivity.EXTRA_PAYMENT_METHOD_NONCE));
                startActivity(intent);
            }
        }
    }

    private void sendNonceToServer(String nonce) {
        RequestParams params = new RequestParams();
        params.put("nonce", nonce);
        new AsyncHttpClient().get(OptionsActivity.getEnvironmentUrl(this) + "/handle_nonce1", params,
                new AsyncHttpResponseHandler() {
                    @Override
                    public void onSuccess(String response) {
                        try {
                            JSONObject res = new JSONObject(response);
                            String mCustomerid = res.getString("customerid");
                            String mToken = res.getString("token");
                            showDialog("Clientid: " + mCustomerid + ". Token:" + mToken);
                        } catch (JSONException e) {
                            showDialog("decode Nonce:"+response);
                        }
                    }

                    @Override
                    public void onFailure(int statusCode, Throwable error, String errorMessage) {
                        showDialog("Unable to get a Nonce token. Status code: " + statusCode + ". Error:" +
                                errorMessage);
                    }
                });
    }

    private void showDialog(String message) {
        new AlertDialog.Builder(this)
                .setMessage(message)
                .show();
    }

}
