package co.com.bnpparibas.cardif.cryptography.service.v1.impl;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

import co.com.bnpparibas.cardif.cryptography.exception.CryptographyException;
import co.com.bnpparibas.cardif.cryptography.model.ws.request.CryptographyRequest;
import co.com.bnpparibas.cardif.cryptography.model.ws.request.CryptographyRequests;
import co.com.bnpparibas.cardif.cryptography.model.ws.request.PlainText;
import co.com.bnpparibas.cardif.cryptography.model.ws.request.Request;
import co.com.bnpparibas.cardif.cryptography.model.ws.request.TransactionStatus;
import co.com.bnpparibas.cardif.cryptography.model.ws.response.Response;
import co.com.bnpparibas.cardif.cryptography.model.ws.response.ResponseDecrypts;
import co.com.bnpparibas.cardif.cryptography.model.ws.response.ResponseEncrypts;
import co.com.bnpparibas.cardif.cryptography.repository.v1.ICryptographyRepository;
import co.com.bnpparibas.cardif.cryptography.service.v1.ICryptographyService;
import co.com.bnpparibas.cardif.cryptography.util.JsonTreePath;

@Service("cryptographyService")
public class CryptographyServiceImpl implements ICryptographyService {

	private static final String CIPHER_INSTANCE = "AES/CBC/NoPadding";
	private static final String ALGORITHM = "AES";

	private static Logger logger = LoggerFactory.getLogger(CryptographyServiceImpl.class);
	@Autowired
	private ICryptographyRepository iCryptographyRepository;

	@Autowired
	private TransactionStatus responseTransaction;

	@Override
	public boolean testPing() throws CryptographyException {
		return this.iCryptographyRepository.testPing();
	}

	@Override
	public String encrypt(CryptographyRequest cryptography) throws CryptographyException {
		String strToEncryptResult = "";
		Cipher cipher = generateCipher(cryptography, Cipher.ENCRYPT_MODE);
		try {
			if (cipher != null) {
				if (cryptography.getType() == null || "".equals(cryptography.getType())
						|| "plain".equals(cryptography.getType())) {
					strToEncryptResult = this.encryptCipher(cryptography.getInformation(), cipher);
				} else if ("json".equals(cryptography.getType())) {
					List<String> pathList = new ArrayList<>();
					JsonTreePath jsonTree = new JsonTreePath();
					pathList = jsonTree.setJsonPaths(cryptography.getInformation(), pathList);
					DocumentContext context = JsonPath.parse(cryptography.getInformation());
					for (String path : pathList) {
						context.set(path, this.encryptCipher("" + context.read(path), cipher));
					}
					strToEncryptResult = context.jsonString();
				}
			}
		} catch (Exception e) {
			logger.error("Error en el metodo encrypt ", e);
			throw new CryptographyException("Error en metodo encrypt");
		}
		return strToEncryptResult;
	}

	private String encryptCipher(String data, Cipher cipher) throws IllegalBlockSizeException, BadPaddingException {
		byte[] plainBytes = data.getBytes();
		int aux = 16;
		if (plainBytes.length > 16) {
			aux = (plainBytes.length % 16 == 0) ? plainBytes.length : ((plainBytes.length / 16) + 1) * 16;
		}
		byte[] paddedBytes = new byte[aux];
		for (int i = 0; i < paddedBytes.length; i++) {
			paddedBytes[i] = (byte) 32;
		}
		System.arraycopy(plainBytes, 0, paddedBytes, 0, plainBytes.length);
		return toHexString(cipher.doFinal(paddedBytes));
	}

	private Cipher generateCipher(CryptographyRequest cryptography, int mode) throws CryptographyException {
		Cipher cipher = null;
		try {
			String key = this.iCryptographyRepository.getKeyValue(cryptography.getKeyName(), "K");
			if (!"".equals(key.trim())) {
				byte[] iv = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
				IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
				SecretKeySpec secretKey = new SecretKeySpec(toByteArray(key), CryptographyServiceImpl.ALGORITHM);
				cipher = Cipher.getInstance(CryptographyServiceImpl.CIPHER_INSTANCE);
				cipher.init(mode, secretKey, ivParameterSpec);
			} else {
				logger.error("No se encontraron llaves o sal con el nombre {}", cryptography.getKeyName());
			}
		} catch (Exception e) {
			logger.error("Error en el metodo generateCipher ", e);
			throw new CryptographyException("Error en metodo generateCipher");
		}
		return cipher;
	}

	public static String toHexString(byte[] array) {
		return DatatypeConverter.printHexBinary(array);
	}

	public static byte[] toByteArray(String hexString) {
		return DatatypeConverter.parseHexBinary(hexString);
	}

	@Override
	public String decrypt(CryptographyRequest cryptographyDecrypt) throws CryptographyException {
		String strToDecryptResult = "";
		Cipher cipher = generateCipher(cryptographyDecrypt, Cipher.DECRYPT_MODE);
		try {
			if (cipher != null) {
				if (cryptographyDecrypt.getType() == null || "".equals(cryptographyDecrypt.getType())
						|| "plain".equals(cryptographyDecrypt.getType())) {
					strToDecryptResult = decryptFormat(cipher, cryptographyDecrypt.getInformation());
				} else if ("json".equals(cryptographyDecrypt.getType())) {
					List<String> pathList = new ArrayList<>();
					JsonTreePath jsonTree = new JsonTreePath();
					pathList = jsonTree.setJsonPaths(cryptographyDecrypt.getInformation(), pathList);
					DocumentContext context = JsonPath.parse(cryptographyDecrypt.getInformation());
					for (String path : pathList) {
						context.set(path, decryptFormat(cipher, "" + context.read(path)));
					}
					strToDecryptResult = context.jsonString();
				}
			}
		} catch (Exception e) {
			logger.error("Error en el metodo decrypt ", e);
			throw new CryptographyException("Error en metodo decrypt");
		}
		return strToDecryptResult;
	}

	private String decryptFormat(Cipher cipher, String txt) throws IllegalBlockSizeException, BadPaddingException {
		return new String(cipher.doFinal(toByteArray(txt)), StandardCharsets.UTF_8).trim().replaceFirst("^0+(?!$)", "");
	}

	@Override
	public List<Request> listCryptograpy(String id, String keyName) throws CryptographyException {
		List<Request> retorno = new ArrayList<>();
		try {
			String response = this.iCryptographyRepository.listCryptograpy(id, keyName);
			JsonElement responseJson = JsonParser.parseString(response);
			JsonArray responseJsonArray = responseJson.getAsJsonArray();

			for (int i = 0; i < responseJsonArray.size(); i++) {
				retorno.add(new Gson().fromJson(responseJsonArray.get(i), Request.class));
			}

		} catch (Exception e) {
			logger.error("Error en el metodo listCryptograpy ", e);
			throw new CryptographyException("Error Consulting list Cryptograpies");
		}
		return retorno;
	}

	@Override
	public TransactionStatus requestUpdatePut(Request requestUpdate) throws CryptographyException {
		Response response = null;
		try {
			responseTransaction = this.iCryptographyRepository.requestUpdatePut(requestUpdate);
			response = new Response();
			response.setStatus("Success Updating Cryptography");
		} catch (Exception e) {
			logger.error("Error en el metodo requestUpdatePut ", e);
			throw new CryptographyException("Error Updating Cryptography");
		}
		return responseTransaction;
	}

	@Override
	public TransactionStatus requestCreate(Request requestCreate) throws CryptographyException {
		try {
			responseTransaction = this.iCryptographyRepository.requestCreate(requestCreate);
		} catch (Exception e) {
			logger.error("Error en el metodo requestCreate ", e);
			throw new CryptographyException("Error Creating Cryptography");
		}
		return responseTransaction;
	}

	@Override
	public TransactionStatus requestDelete(String id) throws CryptographyException {
		try {
			responseTransaction = this.iCryptographyRepository.requestDelete(id);
		} catch (Exception e) {
			logger.error("Error en el metodo requestDelete ", e);
			throw new CryptographyException("Error deleting Cryptography");
		}
		return responseTransaction;
	}

	@Override
	public ArrayList<Object> encrypts(CryptographyRequests cryptographyRequests)
			throws CryptographyException, InvalidKeyException {
		if (cryptographyRequests.getInformation() == null || cryptographyRequests.getInformation().isEmpty())
			throw new InvalidKeyException("flat data array cannot be null or empty");

		ArrayList<Object> retorno = new ArrayList<>();
		cryptographyRequests.getInformation().stream().forEach(plainText -> {
			try {
				String cypherText = encrypt(new CryptographyRequest(cryptographyRequests.getKeyName(), plainText.getText(),
						cryptographyRequests.getCipherType(), ""));
				retorno.add(new ResponseEncrypts(plainText.getName(), cypherText, HttpStatus.OK.value(), HttpStatus.OK.name()));
			} catch (Exception e) {
				retorno.add(new ResponseEncrypts(null, null, HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage()));
			}
		});

		return retorno;
	}

	@Override
	public ArrayList<Object> decrypts(CryptographyRequests cryptographyRequests)
			throws CryptographyException, InvalidKeyException {
		if (cryptographyRequests.getInformation() == null || cryptographyRequests.getInformation().isEmpty())
			throw new InvalidKeyException("flat data array cannot be null or empty");
		ArrayList<Object> retorno = new ArrayList<>();
		
		cryptographyRequests.getInformation().stream().forEach(plainText -> {
			try {
				String cypherText = decrypt(new CryptographyRequest(cryptographyRequests.getKeyName(), plainText.getText(),
						cryptographyRequests.getCipherType(), ""));
				retorno.add(new ResponseDecrypts(plainText.getName(), cypherText, HttpStatus.OK.value(), HttpStatus.OK.name()));
			} catch (Exception e) {
				retorno.add(new ResponseDecrypts(null, null, HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage()));
			}
		});
		return retorno;
	}

}
