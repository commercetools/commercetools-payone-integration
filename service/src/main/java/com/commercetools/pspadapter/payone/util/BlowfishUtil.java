package com.commercetools.pspadapter.payone.util;

import com.google.common.base.Charsets;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * Provides methods for encrypting or decrypting data with
 * Blowfish algorithm (mode: ECB, padding: none).
 *
 * @author fhaertig
 * @since 19.04.16
 */
public class BlowfishUtil {

    private static final String CRYPT_ALGORITHM = "Blowfish";
    private static final String CRYPT_MODE = "ECB";
    private static final String CRYPT_PADDING = "NoPadding";
    private static final Charset CHARSET_UTF_8 = Charsets.UTF_8;

    /**
     * Decrypts a given string encoded in HEX with a given key.
     *
     * @param key the secret key as string which was used to encrypt the data
     * @param encryptedString the encrypted data as String, encoded in HEX
     * @return the decrypted data without padding chars.
     * @throws IllegalArgumentException when the given key is not matching the requirements
     */
    public static String decryptHexToString(final String key, final String encryptedString) {
        SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(CHARSET_UTF_8), CRYPT_ALGORITHM);

        byte[] encryptedHexBinary = DatatypeConverter.parseHexBinary(encryptedString);
        try {
            Cipher cipher = Cipher.getInstance(String.format("%s/%s/%s", CRYPT_ALGORITHM, CRYPT_MODE, CRYPT_PADDING));
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            return new String(cipher.doFinal(encryptedHexBinary), CHARSET_UTF_8).trim();
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new UnsupportedOperationException(String.format("Could not find valid crypt algorithm for %s/%s/%s", CRYPT_ALGORITHM, CRYPT_MODE, CRYPT_PADDING), e);
        } catch (IllegalBlockSizeException e) {
            throw new IllegalArgumentException(String.format("The encrypted data is not a multiple of the block size defined by algorithm %s/%s/%s", CRYPT_ALGORITHM, CRYPT_MODE, CRYPT_PADDING), e);
        } catch (InvalidKeyException | BadPaddingException e) {
            throw new IllegalArgumentException("The given key for decryption seems to be invalid!", e);
        }
    }
}
