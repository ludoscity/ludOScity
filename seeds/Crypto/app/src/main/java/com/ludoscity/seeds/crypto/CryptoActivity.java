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
import org.spongycastle.openpgp.PGPSignature;
import org.spongycastle.openpgp.operator.PGPDigestCalculator;
import org.spongycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder;
import org.spongycastle.openpgp.operator.jcajce.JcaPGPDigestCalculatorProviderBuilder;
import org.spongycastle.openpgp.operator.jcajce.JcaPGPKeyPair;
import org.spongycastle.openpgp.operator.jcajce.JcePBESecretKeyEncryptorBuilder;

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

    private static void exportKeyPair(
            OutputStream secretOut,
            OutputStream    publicOut,
            KeyPair dsaKp,
            KeyPair         elgKp,
            String          identity,
            char[]          passPhrase,
            boolean         armor)
            throws IOException, InvalidKeyException, NoSuchProviderException, SignatureException, PGPException
    {
        if (armor)
        {
            secretOut = new ArmoredOutputStream(secretOut);
        }

        PGPKeyPair dsaKeyPair = new JcaPGPKeyPair(PGPPublicKey.DSA, dsaKp, new Date());
        PGPKeyPair        elgKeyPair = new JcaPGPKeyPair(PGPPublicKey.ELGAMAL_ENCRYPT, elgKp, new Date());
        PGPDigestCalculator sha1Calc = new JcaPGPDigestCalculatorProviderBuilder().build().get(HashAlgorithmTags.SHA1);
        PGPKeyRingGenerator keyRingGen = new PGPKeyRingGenerator(PGPSignature.POSITIVE_CERTIFICATION, dsaKeyPair,
                identity, sha1Calc, null, null, new JcaPGPContentSignerBuilder(dsaKeyPair.getPublicKey().getAlgorithm(), HashAlgorithmTags.SHA1), new JcePBESecretKeyEncryptorBuilder(PGPEncryptedData.AES_256, sha1Calc).setProvider("SC").build(passPhrase));

        keyRingGen.addSubKey(elgKeyPair);

        keyRingGen.generateSecretKeyRing().encode(secretOut);

        secretOut.close();

        if (armor)
        {
            publicOut = new ArmoredOutputStream(publicOut);
        }

        keyRingGen.generatePublicKeyRing().encode(publicOut);

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

                KeyPairGenerator dsaKpg;
                try {
                    dsaKpg = KeyPairGenerator.getInstance("DSA", "SC");

                    dsaKpg.initialize(512);

                    //
                    // this takes a while as the key generator has to generate some DSA params
                    // before it generates the key.
                    //
                    KeyPair             dsaKp = dsaKpg.generateKeyPair();

                    KeyPairGenerator    elgKpg = KeyPairGenerator.getInstance("ELGAMAL", "SC");
                    BigInteger g = new BigInteger("153d5d6172adb43045b68ae8e1de1070b6137005686d29d3d73a7749199681ee5b212c9b96bfdcfa5b20cd5e3fd2044895d609cf9b410b7a0f12ca1cb9a428cc", 16);
                    BigInteger          p = new BigInteger("9494fec095f3b85ee286542b3836fc81a5dd0a0349b4c239dd38744d488cf8e31db8bcb7d33b41abb9e5a33cca9144b1cef332c94bf0573bf047a3aca98cdf3b", 16);

                    DHParameterSpec elParams = new DHParameterSpec(p, g);

                    elgKpg.initialize(elParams);

                    //
                    // this is quicker because we are using pregenerated parameters.
                    //
                    KeyPair                    elgKp = elgKpg.generateKeyPair();


                    //BufferedReader br = new BufferedReader(new output);

                    // First attempt
                    ByteArrayOutputStream secretKey = new ByteArrayOutputStream();
                    ByteArrayOutputStream publicKey = new ByteArrayOutputStream();

                    //776 bytes
                    //exportKeyPair(secretKey, publicKey, dsaKp, elgKp, "identity", "Brilliant pass phrase".toCharArray(), false);
                    //1164 bytes but "human readable"
                    exportKeyPair(secretKey, publicKey, dsaKp, elgKp, "identity", "Brilliant pass phrase".toCharArray(), true);


                    mSecret.setText("size bytes : " + secretKey.size() + '\n' + secretKey.toString());



                    /*ByteArrayOutputStream os = new ByteArrayOutputStream();
                    GZIPOutputStream secretKey = new GZIPOutputStream(os);
                    GZIPOutputStream publicKey = new GZIPOutputStream(new ByteArrayOutputStream());

                    exportKeyPair(secretKey, publicKey, dsaKp, elgKp, "identity", "Brilliant pass phrase".toCharArray(), false);

                    mSecret.setText("size bytes : " + os.toByteArray().length); //799 bytes
                    os.close();*/


                } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException | IOException | SignatureException | PGPException | InvalidKeyException e) {
                    Log.d("CRYPTO_TAG", "oops", e);
                }


            }
        });

        //Security.addProvider(new BouncyCastleProvider());
    }
}
