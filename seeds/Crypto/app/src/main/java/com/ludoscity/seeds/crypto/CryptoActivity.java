package com.ludoscity.seeds.crypto;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.spongycastle.bcpg.ArmoredOutputStream;
import org.spongycastle.bcpg.HashAlgorithmTags;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.openpgp.PGPEncryptedData;
import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPKeyPair;
import org.spongycastle.openpgp.PGPKeyRingGenerator;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSignature;
import org.spongycastle.openpgp.operator.PGPDigestCalculator;
import org.spongycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder;
import org.spongycastle.openpgp.operator.jcajce.JcaPGPDigestCalculatorProviderBuilder;
import org.spongycastle.openpgp.operator.jcajce.JcaPGPKeyPair;
import org.spongycastle.openpgp.operator.jcajce.JcePBESecretKeyEncryptorBuilder;
//import org.spongycastle.openpgp.examples.RSAKeyPairGenerator

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.SignatureException;
import java.util.Date;
import java.util.zip.GZIPOutputStream;

import javax.crypto.spec.DHParameterSpec;

//import org.spongycastle.jce.provider.BouncyCastleProvider;
//import org.spongycastle.openpgp.examples.DSAElGamalKeyRingGenerator

//import java.security.Security;

public class CryptoActivity extends AppCompatActivity {

    private Button mGenButton;
    private TextView mSecret;


    private static final String TAG = "CRYPTO_TAG";

    private static void exportKeyPair(
            OutputStream    secretOut,
            OutputStream    publicOut,
            KeyPair         pair,
            String          identity,
            char[]          passPhrase,
            boolean         armor)
            throws IOException, InvalidKeyException, NoSuchProviderException, SignatureException, PGPException
    {
        if (armor)
        {
            secretOut = new ArmoredOutputStream(secretOut);
        }

        PGPDigestCalculator sha1Calc = new JcaPGPDigestCalculatorProviderBuilder().build().get(HashAlgorithmTags.SHA1);
        PGPKeyPair          keyPair = new JcaPGPKeyPair(PGPPublicKey.RSA_GENERAL, pair, new Date());
        PGPSecretKey        secretKey = new PGPSecretKey(PGPSignature.DEFAULT_CERTIFICATION, keyPair, identity, sha1Calc, null, null, new JcaPGPContentSignerBuilder(keyPair.getPublicKey().getAlgorithm(), HashAlgorithmTags.SHA1), new JcePBESecretKeyEncryptorBuilder(PGPEncryptedData.CAST5, sha1Calc).setProvider("SC").build(passPhrase));

        secretKey.encode(secretOut);

        secretOut.close();

        if (armor)
        {
            publicOut = new ArmoredOutputStream(publicOut);
        }

        PGPPublicKey    key = secretKey.getPublicKey();

        key.encode(publicOut);

        publicOut.close();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crypto);

        mGenButton = (Button)findViewById(R.id.generateKeys);
        mSecret = (TextView)findViewById(R.id.secretKey);

        mGenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Security.addProvider(new BouncyCastleProvider());

                // second test from https://github.com/open-keychain/bouncycastle/blob/openkeychain-master/pg/src/main/java/org/bouncycastle/openpgp/examples/RSAKeyPairGenerator.java
                //adapted for spongycastle

                KeyPairGenerator kpg;
                try {
                    kpg = KeyPairGenerator.getInstance("RSA", "SC");

                    kpg.initialize(512);

                    KeyPair                    kp = kpg.generateKeyPair();

                    //BufferedReader br = new BufferedReader(new output);

                    // First attempt
                    ByteArrayOutputStream secretKey = new ByteArrayOutputStream();
                    ByteArrayOutputStream publicKey = new ByteArrayOutputStream();

                    //393 bytes
                    //exportKeyPair(secretKey, publicKey, dsaKp, elgKp, "identity", "Brilliant pass phrase".toCharArray(), false);
                    //644 bytes but "human readable"
                    exportKeyPair(secretKey, publicKey, kp, "identity", "Brilliant pass phrase".toCharArray(), true);


                    mSecret.setText("size bytes : " + secretKey.size() + '\n' + secretKey.toString());



                    /*ByteArrayOutputStream os = new ByteArrayOutputStream();
                    GZIPOutputStream secretKey = new GZIPOutputStream(os);
                    GZIPOutputStream publicKey = new GZIPOutputStream(new ByteArrayOutputStream());

                    exportKeyPair(secretKey, publicKey, dsaKp, elgKp, "identity", "Brilliant pass phrase".toCharArray(), false);

                    mSecret.setText("size bytes : " + os.toByteArray().length); //799 bytes
                    os.close();*/


                } catch (NoSuchAlgorithmException | NoSuchProviderException | IOException | SignatureException | PGPException | InvalidKeyException e) {
                    Log.d(TAG, "oops", e);
                }


            }
        });

        //Security.addProvider(new BouncyCastleProvider());
    }
}
