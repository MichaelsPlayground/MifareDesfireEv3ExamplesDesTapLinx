package de.androidcrypto.taplinxexample;

import android.annotation.TargetApi;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.textfield.TextInputLayout;
import com.nxp.nfclib.CardType;
import com.nxp.nfclib.KeyType;
import com.nxp.nfclib.NxpNfcLib;
import com.nxp.nfclib.defaultimpl.KeyData;
import com.nxp.nfclib.desfire.DESFireFactory;
import com.nxp.nfclib.desfire.IDESFireEV1;
import com.nxp.nfclib.desfire.IDESFireEV2;
import com.nxp.nfclib.desfire.IDESFireEV3;
import com.nxp.nfclib.exceptions.NxpNfcLibException;
import com.nxp.nfclib.exceptions.PICCException;
import com.nxp.nfclib.interfaces.IKeyData;
import com.nxp.nfclib.utils.NxpLogUtils;
import com.nxp.nfclib.utils.Utilities;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;


public class MainActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback {


    /**
     * Package Key.
     */
    static String packageKey = "07e7a6e1091d445f60ce756883b42ef2";

    /**
     * NxpNfclib instance.
    */
    private NxpNfcLib libInstance = null;

    /**
     * Cipher instance.
     */
    private Cipher cipher = null;
    /**
     * Iv.
     */
    private IvParameterSpec iv = null;

    /**
     * Desfire card object.
     */
    private IDESFireEV1 desFireEV1;

    private IDESFireEV2 desFireEV2;

    private IDESFireEV3 desFireEV3;

    private CardType mCardType = CardType.UnknownCard;

    private static final String KEY_APP_MASTER = "This is my key  ";
    /**
     * bytes key.
     */
    private byte[] bytesKey = new byte[16]; // 16 null bytes for AES

    private com.google.android.material.textfield.TextInputEditText output, errorCode;
    private com.google.android.material.textfield.TextInputLayout errorCodeLayout;

    /**
     * section for temporary actions
     */

    private Button setupCompleteApplication, standardWriteRead, standardWriteReadDefaultKeys;
    private Button getFileSettingsDesfire;

    /**
     * section for general workflow
     */

    private LinearLayout llGeneralWorkflow;
    private Button tagVersion, keySettings, freeMemory, formatPicc, selectMasterApplication;

    /**
     * section for application handling
     */
    private LinearLayout llApplicationHandling;
    private Button applicationList, applicationCreate, applicationSelect, applicationDelete;
    private com.google.android.material.textfield.TextInputEditText numberOfKeys, applicationId, applicationSelected;
    private byte[] selectedApplicationId = null;

    /**
     * section for files handling
     */

    private LinearLayout llFiles;

    private Button fileList, fileSelect, fileDelete;
    private com.google.android.material.textfield.TextInputEditText fileSelected;
    private String selectedFileId = "";
    private int selectedFileIdInt = -1;
    private int selectedFileSize;

    /**
     * section for standard & backup file handling
     */

    private LinearLayout llStandardFile;
    private Button fileStandardCreate, fileStandardWrite, fileStandardRead;
    private com.google.android.material.textfield.TextInputEditText fileSize, fileData;
    private RadioButton rbStandardFile, rbBackupFile;
    private com.shawnlin.numberpicker.NumberPicker npStandardFileId;
    RadioButton rbFileStandardPlainCommunication, rbFileStandardMacedCommunication, rbFileStandardEncryptedCommunication;
    private final int MAXIMUM_STANDARD_DATA_CHUNK = 40; // if any data are longer we create chunks when writing

    //private FileSettings selectedFileSettings;


    /**
     * section for value file handling
     */

    private LinearLayout llValueFile;
    private Button fileValueCreate, fileValueCredit, fileValueDebit, fileValueRead;
    RadioButton rbFileValuePlainCommunication, rbFileValueMacedCommunication, rbFileValueEncryptedCommunication;
    private com.shawnlin.numberpicker.NumberPicker npValueFileId;
    private com.google.android.material.textfield.TextInputEditText lowerLimitValue, upperLimitValue, initialValueValue, creditDebitValue;

    /**
     * section for record file handling
     */

    private LinearLayout llRecordFile;
    private Button fileRecordCreate, fileRecordWrite, fileRecordRead;
    private RadioButton rbLinearRecordFile, rbCyclicRecordFile;
    RadioButton rbFileRecordPlainCommunication, rbFileRecordMacedCommunication, rbFileRecordEncryptedCommunication;
    private com.shawnlin.numberpicker.NumberPicker npRecordFileId;
    private com.google.android.material.textfield.TextInputEditText fileRecordSize, fileRecordData, fileRecordNumberOfRecords;

    /**
     * work with encrypted standard files - EXPERIMENTAL
     */

    private LinearLayout llStandardFileEnc;
    private Button fileStandardCreateEnc, fileStandardWriteEnc, manualEncryption;

    /**
     * section for authentication
     */

    private Button authKeyDM0, authKeyD0, authKeyD1, authKeyD2, authKeyD3, authKeyD4; // M0 is the Master Application Key

    // changed keys
    private Button authKeyDM0C, authKeyD0C, authKeyD1C, authKeyD2C, authKeyD3C, authKeyD4C; // M0 is the Master Application Key


    /**
     * section for key handling
     */

    private Button changeKeyDM0, changeKeyD0, changeKeyD1, changeKeyD2, changeKeyD3, changeKeyD4;

    // virtual card key handling
    private Button authKeyAM0; // M0 is the Master Application Key AES
    private Button changeKeyVc20, authKeyVc20, changeKeyVc21;

    // changed keys
    private Button changeKeyDM0C, changeKeyD0C, changeKeyD1C, changeKeyD2C, changeKeyD3C, changeKeyD4C;

    // constants
    private final byte[] MASTER_APPLICATION_IDENTIFIER = new byte[3]; // '00 00 00'
    private final byte[] MASTER_APPLICATION_KEY_DEFAULT = Utils.hexStringToByteArray("0000000000000000");
    private final byte[] MASTER_APPLICATION_KEY_AES_DEFAULT = Utils.hexStringToByteArray("00000000000000000000000000000000");
    private final byte[] MASTER_APPLICATION_KEY = Utils.hexStringToByteArray("DD00000000000000");
    private final byte MASTER_APPLICATION_KEY_NUMBER = (byte) 0x00;
    private final byte[] APPLICATION_ID_DES = Utils.hexStringToByteArray("A1A2A3");
    private final byte[] DES_DEFAULT_KEY = new byte[8];
    private final byte[] APPLICATION_KEY_MASTER_DEFAULT = Utils.hexStringToByteArray("0000000000000000"); // default DES key with 8 nulls
    private final byte[] APPLICATION_KEY_MASTER = Utils.hexStringToByteArray("D000000000000000");
    private final byte APPLICATION_KEY_MASTER_NUMBER = (byte) 0x00;
    private final byte APPLICATION_MASTER_KEY_SETTINGS = (byte) 0x0f; // amks
    private final byte KEY_NUMBER_RW = (byte) 0x01;
    private final byte[] APPLICATION_KEY_RW_DEFAULT = Utils.hexStringToByteArray("0000000000000000"); // default DES key with 8 nulls
    private final byte[] APPLICATION_KEY_RW = Utils.hexStringToByteArray("D100000000000000");
    private final byte APPLICATION_KEY_RW_NUMBER = (byte) 0x01;
    private final byte[] APPLICATION_KEY_CAR_DEFAULT = Utils.hexStringToByteArray("0000000000000000"); // default DES key with 8 nulls
    private final byte[] APPLICATION_KEY_CAR = Utils.hexStringToByteArray("D200000000000000");
    private final byte APPLICATION_KEY_CAR_NUMBER = (byte) 0x02;

    private final byte[] APPLICATION_KEY_R_DEFAULT = Utils.hexStringToByteArray("0000000000000000"); // default DES key with 8 nulls
    private final byte[] APPLICATION_KEY_R = Utils.hexStringToByteArray("D300000000000000");
    private final byte APPLICATION_KEY_R_NUMBER = (byte) 0x03;

    private final byte[] APPLICATION_KEY_W_DEFAULT = Utils.hexStringToByteArray("0000000000000000"); // default DES key with 8 nulls
    //private final byte[] APPLICATION_KEY_W = Utils.hexStringToByteArray("B400000000000000");
    private final byte[] APPLICATION_KEY_W = Utils.hexStringToByteArray("D400000000000000");
    private final byte APPLICATION_KEY_W_NUMBER = (byte) 0x04;

    private final byte[] VIRTUAL_CARD_KEY_CONFIG_DEFAULT = Utils.hexStringToByteArray("00000000000000000000000000000000");
    private final byte[] VIRTUAL_CARD_KEY_CONFIG = Utils.hexStringToByteArray("20200000000000000000000000000000");
    private final byte VIRTUAL_CARD_KEY_CONFIG_NUMBER = (byte) 0x20;
    private final byte[] VIRTUAL_CARD_KEY_PROXIMITY_DEFAULT = Utils.hexStringToByteArray("00000000000000000000000000000000");
    private final byte[] VIRTUAL_CARD_KEY_PROXIMITY = Utils.hexStringToByteArray("20200000000000000000000000000000");
    private final byte VIRTUAL_CARD_KEY_PROXIMITY_NUMBER = (byte) 0x21;

    private final byte STANDARD_FILE_NUMBER = (byte) 0x01;


    int COLOR_GREEN = Color.rgb(0, 255, 0);
    int COLOR_RED = Color.rgb(255, 0, 0);

    // variables for NFC handling

    private NfcAdapter mNfcAdapter;
    private IsoDep isoDep;
    private byte[] tagIdByte;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(myToolbar);

        output = findViewById(R.id.etOutput);
        errorCode = findViewById(R.id.etErrorCode);
        errorCodeLayout = findViewById(R.id.etErrorCodeLayout);

        // temporary workflow
        setupCompleteApplication = findViewById(R.id.btnSetupCompleteApplication);
        standardWriteRead = findViewById(R.id.btnStandardFileWriteRead);
        //standardWriteReadDefaultKeys = findViewById(R.id.btnStandardFileWriteReadDefaultKeys);
        getFileSettingsDesfire = findViewById(R.id.btnGetFileSettings);


        // general workflow
        tagVersion = findViewById(R.id.btnGetTagVersion);
        keySettings = findViewById(R.id.btnGetKeySettings);
        freeMemory = findViewById(R.id.btnGetFreeMemory);
        formatPicc = findViewById(R.id.btnFormatPicc);
        selectMasterApplication = findViewById(R.id.btnSelectMasterApplication);

        // application handling
        llApplicationHandling = findViewById(R.id.llApplications);
        applicationList = findViewById(R.id.btnListApplications);
        applicationCreate = findViewById(R.id.btnCreateApplication);
        applicationSelect = findViewById(R.id.btnSelectApplication);
        applicationDelete = findViewById(R.id.btnDeleteApplication);
        applicationSelected = findViewById(R.id.etSelectedApplicationId);
        numberOfKeys = findViewById(R.id.etNumberOfKeys);
        applicationId = findViewById(R.id.etApplicationId);

        // files handling
        fileList = findViewById(R.id.btnListFiles);
        fileSelect = findViewById(R.id.btnSelectFile);
        fileDelete = findViewById(R.id.btnDeleteFile);

        // standard & backup file handling
        llStandardFile = findViewById(R.id.llStandardFile);
        fileStandardCreate = findViewById(R.id.btnCreateStandardFile);
        fileStandardWrite = findViewById(R.id.btnWriteStandardFile);
        fileStandardRead = findViewById(R.id.btnReadStandardFile);
        npStandardFileId = findViewById(R.id.npStandardFileId);
        rbStandardFile = findViewById(R.id.rbStandardFile);
        rbBackupFile = findViewById(R.id.rbBackupFile);
        rbFileStandardPlainCommunication = findViewById(R.id.rbFileStandardPlainCommunication);
        rbFileStandardMacedCommunication = findViewById(R.id.rbFileStandardMacedCommunication);
        rbFileStandardEncryptedCommunication = findViewById(R.id.rbFileStandardEncryptedCommunication);
        fileSize = findViewById(R.id.etFileStandardSize);
        fileData = findViewById(R.id.etFileStandardData);
        fileSelected = findViewById(R.id.etSelectedFileId);

        // value file handling
        llValueFile = findViewById(R.id.llValueFile);
        fileValueCreate = findViewById(R.id.btnCreateValueFile);
        fileValueRead = findViewById(R.id.btnReadValueFile);
        fileValueCredit = findViewById(R.id.btnCreditValueFile);
        fileValueDebit = findViewById(R.id.btnDebitValueFile);
        npValueFileId = findViewById(R.id.npValueFileId);
        rbFileValuePlainCommunication = findViewById(R.id.rbFileValuePlainCommunication);
        rbFileValueMacedCommunication = findViewById(R.id.rbFileValueMacedCommunication);
        rbFileValueEncryptedCommunication = findViewById(R.id.rbFileValueEncryptedCommunication);
        lowerLimitValue = findViewById(R.id.etValueLowerLimit);
        upperLimitValue = findViewById(R.id.etValueUpperLimit);
        initialValueValue = findViewById(R.id.etValueInitialValue);
        creditDebitValue = findViewById(R.id.etValueCreditDebitValue);

        // record file handling
        llRecordFile = findViewById(R.id.llRecordFile);
        fileRecordCreate = findViewById(R.id.btnCreateRecordFile);
        fileRecordRead = findViewById(R.id.btnReadRecordFile);
        fileRecordWrite = findViewById(R.id.btnWriteRecordFile);
        npRecordFileId = findViewById(R.id.npRecordFileId);
        rbFileRecordPlainCommunication = findViewById(R.id.rbFileRecordPlainCommunication);
        rbFileRecordMacedCommunication = findViewById(R.id.rbFileRecordMacedCommunication);
        rbFileRecordEncryptedCommunication = findViewById(R.id.rbFileRecordEncryptedCommunication);
        fileRecordSize = findViewById(R.id.etRecordFileSize);
        fileRecordNumberOfRecords = findViewById(R.id.etRecordFileNumberRecords);
        fileRecordData = findViewById(R.id.etRecordFileData);
        rbLinearRecordFile = findViewById(R.id.rbLinearRecordFile);
        rbCyclicRecordFile = findViewById(R.id.rbCyclicRecordFile);

        // encrypted standard file handling
        llStandardFileEnc = findViewById(R.id.llStandardFileEnc);
        fileStandardCreateEnc = findViewById(R.id.btnCreateStandardFileEnc);
        fileStandardWriteEnc = findViewById(R.id.btnWriteStandardFileEnc);
        manualEncryption = findViewById(R.id.btnManualEnc);

        // authentication handling
        authKeyDM0 = findViewById(R.id.btnAuthDM0);
        authKeyD0 = findViewById(R.id.btnAuthD0);
        authKeyD1 = findViewById(R.id.btnAuthD1);
        authKeyD2 = findViewById(R.id.btnAuthD2);
        authKeyD3 = findViewById(R.id.btnAuthD3);
        authKeyD4 = findViewById(R.id.btnAuthD4);
        // now with changed keys
        authKeyDM0C = findViewById(R.id.btnAuthDM0C);
        authKeyD0C = findViewById(R.id.btnAuthD0C);
        authKeyD1C = findViewById(R.id.btnAuthD1C);
        authKeyD2C = findViewById(R.id.btnAuthD2C);
        authKeyD3C = findViewById(R.id.btnAuthD3C);
        authKeyD4C = findViewById(R.id.btnAuthD4C);

        // key handling
        changeKeyDM0 = findViewById(R.id.btnChangeKeyDM0);
        changeKeyD0 = findViewById(R.id.btnChangeKeyD0);
        changeKeyD1 = findViewById(R.id.btnChangeKeyD1);
        changeKeyD2 = findViewById(R.id.btnChangeKeyD2);
        changeKeyD3 = findViewById(R.id.btnChangeKeyD3);
        changeKeyD4 = findViewById(R.id.btnChangeKeyD4);

        // virtual card key handling
        changeKeyVc20 = findViewById(R.id.btnChangeKeyA20);
        authKeyAM0 = findViewById(R.id.btnAuthAM0);
        authKeyVc20 = findViewById(R.id.btnAuthA20);
        changeKeyVc21 = findViewById(R.id.btnChangeKeyA21);

        // now with changed keys
        changeKeyDM0C = findViewById(R.id.btnChangeKeyDM0C);
        changeKeyD0C = findViewById(R.id.btnChangeKeyD0C);
        changeKeyD1C = findViewById(R.id.btnChangeKeyD1C);
        changeKeyD2C = findViewById(R.id.btnChangeKeyD2C);
        changeKeyD3C = findViewById(R.id.btnChangeKeyD3C);
        changeKeyD4C = findViewById(R.id.btnChangeKeyD4C);

        /* Initialize the library and register to this activity */
        initializeLibrary();

        /* Initialize the Cipher and init vector of 16 bytes with 0xCD */
        initializeCipherinitVector();



        //allLayoutsInvisible(); // default

        // hide soft keyboard from showing up on startup
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);


    }

    /**
     * DESFire Pre Conditions.
     * <p/>
     * PICC Master key should be factory default settings, (ie 16 byte All zero
     * Key ).
     * <p/>
     */
    private void desfireEV1CardLogic() {
        writeToUiAppend(output, "desfireEV1CardLogic Card Detected : " + desFireEV1.getType().getTagName());

        try {
            int timeOut = 2000;
            desFireEV1.getReader().setTimeout(timeOut);
            writeToUiAppend(output,
                    "Version of the Card : "
                            + Utilities.dumpBytes(desFireEV1.getVersion()));
            desFireEV1.selectApplication(0); // todo do this before getApplicationIds
            writeToUiAppend(output,
                    "Existing Applications Ids : " + Arrays.toString(desFireEV1.getApplicationIDs()));

            //desFireEV1.selectApplication(0);

            //desFireEV1.authenticate(0, IDESFireEV1.AuthType.Native, KeyType.THREEDES, objKEY_2KTDES);

            desFireEV1.getReader().close();

            // Set the custom path where logs will get stored, here we are setting the log folder DESFireLogs under
            // external storage.
            String spath = Environment.getExternalStorageDirectory().getPath() + File.separator + "DESFireLogs";
            NxpLogUtils.setLogFilePath(spath);
            // if you don't call save as below , logs will not be saved.
            NxpLogUtils.save();

        } catch (Exception e) {
            writeToUiAppend(output, "IOException occurred... check LogCat");
            e.printStackTrace();
        }

    }

    private void desfireEV2CardLogic() {
        int timeOut = 2000;
        writeToUiAppend(output, "desfireEV2CardLogic Card Detected : " + desFireEV2.getType().getTagName());
        try {
            desFireEV2.getReader().setTimeout(timeOut);
            writeToUiAppend(output, "Version of the Card : "
                            + Utilities.dumpBytes(desFireEV2.getVersion()));
            desFireEV2.selectApplication(0); // todo run this before get applications

            writeToUiAppend(output,
                    "Existing Applications Ids : " + Arrays.toString(desFireEV2.getApplicationIDs()));

            //desFireEV2.selectApplication(0);

            //desFireEV2.authenticate(0, IDESFireEV2.AuthType.Native, KeyType.THREEDES, objKEY_2KTDES);

            // Set the custom path where logs will get stored, here we are setting the log folder DESFireLogs under
            // external storage.
            String spath = Environment.getExternalStorageDirectory().getPath() + File.separator + "DESFireLogs";
            Log.i("LogNXP", "path to logs :" + spath);

            NxpLogUtils.setLogFilePath(spath);
            // if you don't call save as below , logs will not be saved.
            NxpLogUtils.save();

        } catch (Exception e) {
            writeToUiAppend(output, "IOException occurred... check LogCat");
            e.printStackTrace();
        }
    }

    private void desfireEV3CardLogic() {
        int timeOut = 2000;
        writeToUiAppend(output, "desfireEV3CardLogic Card Detected : " + desFireEV3.getType().getTagName());
        try {
            desFireEV3.getReader().setTimeout(timeOut);
            writeToUiAppend(output, "Version of the Card : "
                    + Utilities.dumpBytes(desFireEV3.getVersion()));
            desFireEV3.selectApplication(0); // todo run this before get applications

            writeToUiAppend(output,
                    "Existing Applications Ids : " + Arrays.toString(desFireEV3.getApplicationIDs()));

            int[] appIdsInt = desFireEV3.getApplicationIDs();
            int lastAppIdInt = 0;
            writeToUiAppend(output, "number of applications on the card: " + appIdsInt.length);
            for (int i = 0; i < appIdsInt.length; i++) {
                int appIdInt = appIdsInt[i];
                byte[] appId = Utilities.intToBytes(appIdInt, 3);
                String appIdString = Utilities.dumpBytes(appId);
                writeToUiAppend(output, "i: " + i + " appIdInt: " + appIdInt + " hex: " + appIdString);
                lastAppIdInt = appIdInt;
            }
            // select the last application
            desFireEV3.selectApplication(lastAppIdInt);
            byte[] fileIds = desFireEV3.getFileIDs();
            writeToUiAppend(output, "appId files: " + Utilities.dumpBytes(fileIds));

            String authStatus = desFireEV3.getAuthStatus();
            writeToUiAppend(output, "authStatus: " + authStatus);

            // authenticate with read access key = 3
            int readAccessKeyNumber = 3;

            byte[] TDES_KEY_ZERO = new byte[16]; // 16 bytes even for single des key (double the key)
            KeyData objKEY_TDES_ZERO = new KeyData();
            SecretKeySpec secretKeySpecTDesZero = new SecretKeySpec(TDES_KEY_ZERO, "TDES");
            objKEY_TDES_ZERO.setKey(secretKeySpecTDesZero);

            IKeyData objKEY_2KTDES;
            //desFireEV3.authenticate(readAccessKeyNumber, IDESFireEV3.AuthType.Native, KeyType.THREEDES, objKEY_2KTDES);
            desFireEV3.authenticate(readAccessKeyNumber, IDESFireEV3.AuthType.Native, KeyType.THREEDES, objKEY_TDES_ZERO);
            authStatus = desFireEV3.getAuthStatus();
            writeToUiAppend(output, "authStatus: " + authStatus);

/*
void changeKey(int cardkeyNumber,
               KeyType keyType,
               byte[] oldKey,
               byte[] newKey,
               byte newKeyVersion)
This method allows to change any key stored on the PICC. If the AID 0x00 is selected (PICC level ), the change applies to the PICC Master Key. As only one PICC Master key is stored on MIFARE DESFire EV1. In all other cases (if the selected AID is not 0x00 ) the change applies to the specified KeyNo within the currently selected application ( represented by it's AID ). On Application level ( the selected AID is not 0x00) it is not possible to change key after application creation. NOTE: oldkey and newKey is taken as byte array instead of IKeyData.This is because changing the the Key in the card require actual key bytes. IKeyData represents the secure key object which may be in the secure environment [ like HSM (Hardware secure module)] where we cant get the key contents always.

Parameters:
cardkeyNumber - key number to change.
keyType - Key type of the new Key
oldKey - old key of length 16/24 bytes depends on key type.
if type is AES128 then, key length should be 16 bytes.
if type is THREEDES then, [0 to 7 ] equal to [ 8 to 15 ] bytes of the 16 byte key.
if type is TWO_KEY_THREEDES then, [0 to 7 ] not equal to [ 8 to 15 ] bytes of the 16 byte key.
if type is THREE_KEY_THREEDES then, key data should be 24 bytes but key data not necessarily follow the pattern explained for THREEDES, TWO_KEY_THREEDES
newKey - new key of length 16/24 bytes depends on key type.
if type is AES128 then, key length should be 16 bytes.
if type is THREEDES then, [0 to 7 ] equal to [ 8 to 15 ] bytes of the 16 byte key.
if type is TWO_KEY_THREEDES then, [0 to 7 ] not equal to [ 8 to 15 ] bytes of the 16 byte key.
if type is THREE_KEY_THREEDES then, key data should be 24 bytes but key data not necessarily follow the pattern explained for THREEDES, TWO_KEY_THREEDES
newKeyVersion - new key version byte.
 */


            int fileNumber = 12;
            int offset = 0;
            int readLength = 0; // if 0 the complete file is read
            byte[] dataRead = desFireEV3.readData(fileNumber, offset, readLength);
            writeToUiAppend(output, printData("dataRead", dataRead));
            writeToUiAppend(output, new String(dataRead, StandardCharsets.UTF_8));

            //desFireEV2.selectApplication(0);

            //desFireEV2.authenticate(0, IDESFireEV2.AuthType.Native, KeyType.THREEDES, objKEY_2KTDES);

            // Set the custom path where logs will get stored, here we are setting the log folder DESFireLogs under
            // external storage.
            String spath = Environment.getExternalStorageDirectory().getPath() + File.separator + "DESFireLogs";
            Log.i("LogNXP", "path to logs :" + spath);

            NxpLogUtils.setLogFilePath(spath);
            // if you don't call save as below , logs will not be saved.
            NxpLogUtils.save();
        } catch (SecurityException e) {
            writeToUiAppendBorderColor(errorCode, errorCodeLayout, "SecurityException occurred\n" + e.getMessage(), COLOR_RED);
        } catch (PICCException e) {
            writeToUiAppendBorderColor(errorCode, errorCodeLayout, "PICCException occurred\n" + e.getMessage(), COLOR_RED);
        } catch (Exception e) {
            writeToUiAppend(output, "IOException occurred... check LogCat");
            writeToUiAppendBorderColor(errorCode, errorCodeLayout, "IOException occurred\n" + e.getMessage(), COLOR_RED);
            e.printStackTrace();
        }
    }




    private void cardLogic(final Tag tag) {
        CardType type = CardType.UnknownCard;
        try {
            type = libInstance.getCardType(tag);
        } catch (NxpNfcLibException ex) {
            Toast.makeText(this, ex.getMessage(), Toast.LENGTH_SHORT).show();
        }

        switch (type) {

            case DESFireEV1:
                mCardType = CardType.DESFireEV1;
                desFireEV1 = DESFireFactory.getInstance().getDESFire(libInstance.getCustomModules());
                try {

                    desFireEV1.getReader().connect();
                    desFireEV1.getReader().setTimeout(2000);
                    desfireEV1CardLogic();

                } catch (Throwable t) {
                    t.printStackTrace();
                    writeToUiAppend(output, "Unknown Error Tap Again!");
                }
                break;

            case DESFireEV2:
                mCardType = CardType.DESFireEV2;
                writeToUiAppend(output, "DESFireEV2 Card detected.");
                writeToUiAppend(output, "Card Detected : DESFireEV2");
                desFireEV2 = DESFireFactory.getInstance().getDESFireEV2(libInstance.getCustomModules());
                try {
                    desFireEV2.getReader().connect();
                    desFireEV2.getReader().setTimeout(2000);
                    desfireEV2CardLogic();
                    //desfireEV2CardLogicCustom(); // seems not to work

                } catch (Throwable t) {
                    t.printStackTrace();
                    writeToUiAppend(output, "Unknown Error Tap Again!");
                }
                break;

            case DESFireEV3: // ### todo added without changing classes
                mCardType = CardType.DESFireEV3;
                writeToUiAppend(output, "DESFireEV3 Card detected.");

                writeToUiAppend(output, "Card Detected : DESFireEV3");
                desFireEV3 = DESFireFactory.getInstance().getDESFireEV3(libInstance.getCustomModules());
                try {
                    desFireEV3.getReader().connect();
                    desFireEV3.getReader().setTimeout(2000);
                    //desfireEV3SetVcConfigurationKey();
                    desfireEV3CardLogic();
                    //desfireEV3CardLogicProximityCheck();
                    //desfireEV3CardLogicCustom();

                } catch (Throwable t) {
                    t.printStackTrace();
                    writeToUiAppend(output, "Unknown Error Tap Again!");
                }
                break;

        }
    }

    /**
     * Initialize the library and register to this activity.
     */
    @TargetApi(19)
    private void initializeLibrary() {
        libInstance = NxpNfcLib.getInstance();
        try {
            libInstance.registerActivity(this, packageKey);
        } catch (NxpNfcLibException ex) {
            Toast.makeText(this, ex.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Initialize the Cipher and init vector of 16 bytes with 0xCD.
     */

    private void initializeCipherinitVector() {

        /* Initialize the Cipher */
        try {
            cipher = Cipher.getInstance("AES/CBC/NoPadding");
        } catch (NoSuchAlgorithmException e) {

            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        }

        /* set Application Master Key */
        bytesKey = KEY_APP_MASTER.getBytes();

        /* Initialize init vector of 16 bytes with 0xCD. It could be anything */
        byte[] ivSpec = new byte[16];
        Arrays.fill(ivSpec, (byte) 0xCD);
        iv = new IvParameterSpec(ivSpec);

    }




    /**
     * section for NFC handling
     */

    // This method is run in another thread when a card is discovered
    // !!!! This method cannot cannot direct interact with the UI Thread
    // Use `runOnUiThread` method to change the UI from this method
    @Override
    public void onTagDiscovered(Tag tag) {

        writeToUiAppend(output, "NFC tag discovered");

        cardLogic(tag);

/*
        isoDep = null;
        try {
            isoDep = IsoDep.get(tag);
            if (isoDep != null) {

                // Make a Sound
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(VibrationEffect.createOneShot(150, 10));
                } else {
                    Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    v.vibrate(200);
                }

                runOnUiThread(() -> {
                    output.setText("");
                    //output.setBackgroundColor(getResources().getColor(R.color.white));
                });
                isoDep.connect();
                // get tag ID
                tagIdByte = tag.getId();
                writeToUiAppend(output, "tag id: " + Utils.bytesToHex(tagIdByte));
                writeToUiAppend(output, "NFC tag connected");

            }

        } catch (IOException e) {
            writeToUiAppend(output, "ERROR: IOException " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
*/
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mNfcAdapter != null) {

            Bundle options = new Bundle();
            // Work around for some broken Nfc firmware implementations that poll the card too fast
            options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250);

            // Enable ReaderMode for all types of card and disable platform sounds
            // the option NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK is NOT set
            // to get the data of the tag afer reading
            mNfcAdapter.enableReaderMode(this,
                    this,
                    NfcAdapter.FLAG_READER_NFC_A |
                            NfcAdapter.FLAG_READER_NFC_B |
                            NfcAdapter.FLAG_READER_NFC_F |
                            NfcAdapter.FLAG_READER_NFC_V |
                            NfcAdapter.FLAG_READER_NFC_BARCODE |
                            NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS,
                    options);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mNfcAdapter != null)
            mNfcAdapter.disableReaderMode(this);
    }

    /**
     * section for layout handling
     */
    private void allLayoutsInvisible() {
        // todo change this
        //llApplicationHandling.setVisibility(View.GONE);
        //llStandardFile.setVisibility(View.GONE);
    }

    /**
     * section for UI handling
     */

    private void writeToUiAppend(TextView textView, String message) {
        runOnUiThread(() -> {
            String oldString = textView.getText().toString();
            if (TextUtils.isEmpty(oldString)) {
                textView.setText(message);
            } else {
                String newString = message + "\n" + oldString;
                textView.setText(newString);
                System.out.println(message);
            }
        });
    }

    private void writeToUiAppendBorderColor(TextView textView, TextInputLayout textInputLayout, String message, int color) {
        runOnUiThread(() -> {

            // set the color to green
            //Color from rgb
            // int color = Color.rgb(255,0,0); // red
            //int color = Color.rgb(0,255,0); // green
            //Color from hex string
            //int color2 = Color.parseColor("#FF11AA"); light blue
            int[][] states = new int[][]{
                    new int[]{android.R.attr.state_focused}, // focused
                    new int[]{android.R.attr.state_hovered}, // hovered
                    new int[]{android.R.attr.state_enabled}, // enabled
                    new int[]{}  //
            };
            int[] colors = new int[]{
                    color,
                    color,
                    color,
                    //color2
                    color
            };
            ColorStateList myColorList = new ColorStateList(states, colors);
            textInputLayout.setBoxStrokeColorStateList(myColorList);

            String oldString = textView.getText().toString();
            if (TextUtils.isEmpty(oldString)) {
                textView.setText(message);
            } else {
                String newString = message + "\n" + oldString;
                textView.setText(newString);
                System.out.println(message);
            }
        });
    }

    public String printData(String dataName, byte[] data) {
        int dataLength;
        String dataString = "";
        if (data == null) {
            dataLength = 0;
            dataString = "IS NULL";
        } else {
            dataLength = data.length;
            dataString = Utils.bytesToHex(data);
        }
        StringBuilder sb = new StringBuilder();
        sb
                .append(dataName)
                .append(" length: ")
                .append(dataLength)
                .append(" data: ")
                .append(dataString);
        return sb.toString();
    }

    private void clearOutputFields() {
        output.setText("");
        errorCode.setText("");
        // reset the border color to primary for errorCode
        int color = R.color.colorPrimary;
        int[][] states = new int[][]{
                new int[]{android.R.attr.state_focused}, // focused
                new int[]{android.R.attr.state_hovered}, // hovered
                new int[]{android.R.attr.state_enabled}, // enabled
                new int[]{}  //
        };
        int[] colors = new int[]{
                color,
                color,
                color,
                color
        };
        ColorStateList myColorList = new ColorStateList(states, colors);
        errorCodeLayout.setBoxStrokeColorStateList(myColorList);
    }

    /**
     * section for options menu
     */

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_activity_main, menu);

        MenuItem mApplications = menu.findItem(R.id.action_applications);
        mApplications.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                allLayoutsInvisible();
                llApplicationHandling.setVisibility(View.VISIBLE);
                return false;
            }
        });

        MenuItem mStandardFile = menu.findItem(R.id.action_standard_file);
        mStandardFile.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                allLayoutsInvisible();
                llStandardFile.setVisibility(View.VISIBLE);
                return false;
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

}