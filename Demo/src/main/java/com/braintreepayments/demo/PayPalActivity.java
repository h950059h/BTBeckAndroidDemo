package com.braintreepayments.demo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import com.braintreepayments.api.Braintree;
import com.braintreepayments.api.Braintree.PaymentMethodNonceListener;
import com.braintreepayments.api.dropin.BraintreePaymentActivity;
import com.braintreepayments.api.dropin.view.PaymentButton;

public class PayPalActivity extends Activity implements PaymentMethodNonceListener {

    private Braintree mBraintree;
    private PaymentButton mPaymentButton;

    protected void onCreate(Bundle onSaveInstanceState) {
        super.onCreate(onSaveInstanceState);
        setContentView(R.layout.paypal);

        //mPaymentButton = (PaymentButton) findViewById(R.id.payment_button);

        mBraintree = Braintree.getInstance(this,
                getIntent().getStringExtra(BraintreePaymentActivity.EXTRA_CLIENT_TOKEN));
        mBraintree.addListener(this);
        //mPaymentButton.initialize(this, mBraintree);
        Button purbutton = (Button) findViewById(R.id.purchase_button);
        ImageButton ppbutton = (ImageButton) findViewById(R.id.pp_button);
        ppbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //PayPal
                mBraintree.startPayWithPayPal(PayPalActivity.this, PaymentButton.REQUEST_CODE);
            }
        });
    }

    @Override
    public void onPaymentMethodNonce(String paymentMethodNonce) {
        setResult(RESULT_OK, new Intent()
                .putExtra(BraintreePaymentActivity.EXTRA_PAYMENT_METHOD_NONCE, paymentMethodNonce));
        finish();
    }

    @Override
    public void onActivityResult(int requestCode, int responseCode, Intent data) {
        if (requestCode == PaymentButton.REQUEST_CODE) {
            if (responseCode == RESULT_OK) {
                mBraintree.finishPayWithPayPal(this, responseCode, data);
            }
        }
    }

    public void onClick(View view) {
        //PayPal
        mBraintree.startPayWithPayPal(this, PaymentButton.REQUEST_CODE/*PAYPAL_REQUEST_CODE*/);

    }

}
