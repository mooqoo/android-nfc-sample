package practice.example.com.practice_nfc;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.TagLostException;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Random;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class MainActivity extends Activity {
    public static final String TAG = "NFC";
    public static final String MIME_TEXT_PLAIN = "text/plain";


    //Global Variable
    private NfcAdapter mNfcAdapter;
    public boolean isWrite = false;

    //Bind View
    @Bind(R.id.tv_log) public TextView tv_log;
    @OnClick({R.id.btn_read_nfc, R.id.btn_write_nfc})
    public void OnClick(View view) {
        switch(view.getId()) {
            case R.id.btn_read_nfc:
                isWrite = false;
                updateLog("btn Read: isWrite=" +isWrite);
                break;
            case R.id.btn_write_nfc:
                isWrite = true;
                updateLog("btn Read: isWrite=" + isWrite);
                break;
            default:
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //bind view using butterknife
        ButterKnife.bind(this);
        tv_log.setMovementMethod(new ScrollingMovementMethod());

        //check NFC availability and if it is enabled
        verify_NFC();
    }

    //we need to check whether the user's device have NFC.
    private void verify_NFC() {
        Log.i(TAG,"verify_NFC...");

        //check NFC availability
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if(mNfcAdapter==null) {
            Toast.makeText(this,"NFC is not supported with this device.", Toast.LENGTH_SHORT).show();
            updateLog("NFC is not supported with this device.");
            finish();
            return;
        }

        if(!mNfcAdapter.isEnabled()) {
            updateLog("NFC is disabled.");
            enableNFC();
        } else {
            updateLog("NFC is enabled");
        }
    }

    //let the user enable NFC
    private void enableNFC() {
        AlertDialog.Builder alertbox = new AlertDialog.Builder(this);
        alertbox.setTitle("NFC not enabled");
        alertbox.setMessage("Please enable NFC if you want to use nfc to verify the coupon!");
        alertbox.setPositiveButton("Turn On", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //if current api level is 16 or above: use ACTION_NFC_SETTINGS
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    updateLog("enableNFC: ACTION_NFC_SETTINGS");
                    //Intent intent = new Intent(Settings.ACTION_NFC_SETTINGS);
                    startActivityForResult(new Intent(Settings.ACTION_NFC_SETTINGS), 0);
                    //startActivity(intent);

                } else {
                    updateLog("enableNFC: ACTION_WIRELESS_SETTINGS");
                    //Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
                    startActivityForResult(new Intent(Settings.ACTION_WIRELESS_SETTINGS), 0);
                    //startActivity(intent);
                }
            }
        });
        alertbox.setNegativeButton("Close", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        alertbox.show();

        /*
        //if current api level is 16 or above: use ACTION_NFC_SETTINGS
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            updateLog("enableNFC: ACTION_NFC_SETTINGS");
            //Intent intent = new Intent(Settings.ACTION_NFC_SETTINGS);
            startActivityForResult(new Intent(Settings.ACTION_NFC_SETTINGS), 0);
            //startActivity(intent);

        } else {
            updateLog("enableNFC: ACTION_WIRELESS_SETTINGS");
            //Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
            startActivityForResult(new Intent(Settings.ACTION_WIRELESS_SETTINGS), 0);
            //startActivity(intent);
        }
        */
    }


    private void updateLog(String msg) {
        if(tv_log==null)
            return;
        tv_log.setText(tv_log.getText() + msg + "\n");
    }


    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume is called");

        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
            Log.i(TAG,"onResume(): NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())");
            handleIntent(getIntent());
        }

        /**
         * It's important, that the activity is in the foreground (resumed). Otherwise
         * an IllegalStateException is thrown.
         */
        setupForegroundDispatch(this, mNfcAdapter);
    }

    @Override
    protected void onPause() {
        /**
         * Call this before onPause, otherwise an IllegalArgumentException is thrown as well.
         */
        stopForegroundDispatch(this, mNfcAdapter);
        super.onPause();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        /**
         * This method gets called, when a new Intent gets associated with the current activity instance.
         * Instead of creating a new activity, onNewIntent will be called. For more information have a look
         * at the documentation.
         *
         * In our case this method gets called, when the user attaches a Tag to the device.
         */
        Log.i(TAG,"onNewIntent()");
        handleIntent(intent);
        setIntent(new Intent());    //since we already handle the intent, set it to null, so onResume wont handle it again.
    }

    private void handleIntent(Intent intent) {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action) ||
                NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {

            String type = intent.getType();
            updateLog("handleIntent: type = " + type);
            if (MIME_TEXT_PLAIN.equals(type)) {
                Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                if(isWrite) {
                    updateLog("writeTag...");
                    writeTag(this, tag, "test write " + new Random().nextInt(50));
                } else {
                    updateLog("readTag...");
                    new NdefReaderTask().execute(tag);
                }
            } else {
                //write if it is other type
                if(isWrite) {
                    updateLog("writeTag...");
                    Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                    writeTag(this, tag, "test write " + new Random().nextInt(50));
                }   else {
                    updateLog("Wrong mime type: " + type);
                    Log.d(TAG, "Wrong mime type: " + type);
                }
            }
        }
    }

    /**
     * @param activity The corresponding {@link Activity} requesting the foreground dispatch.
     * @param adapter The {@link NfcAdapter} used for the foreground dispatch.
     */
    public static void setupForegroundDispatch(final Activity activity, NfcAdapter adapter) {
        final Intent intent = new Intent(activity.getApplicationContext(), activity.getClass());
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        final PendingIntent pendingIntent = PendingIntent.getActivity(activity.getApplicationContext(), 0, intent, 0);

        IntentFilter[] filters = new IntentFilter[1];
        String[][] techList = new String[][]{};

        // Notice that this is the same filter as in our manifest.
        filters[0] = new IntentFilter();
        filters[0].addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
        filters[0].addCategory(Intent.CATEGORY_DEFAULT);
        try {
            filters[0].addDataType(MIME_TEXT_PLAIN);
        } catch (MalformedMimeTypeException e) {
            throw new RuntimeException("Check your mime type.");
        }

        adapter.enableForegroundDispatch(activity, pendingIntent, filters, techList);
    }

    /**
     * @param activity The corresponding {@link Activity} requesting to stop the foreground dispatch.
     * @param adapter The {@link NfcAdapter} used for the foreground dispatch.
     */
    public static void stopForegroundDispatch(final Activity activity, NfcAdapter adapter) {
        adapter.disableForegroundDispatch(activity);
    }


    /**
     * Background task for reading the data. Do not block the UI thread while reading.
     *
     * @author Ralf Wondratschek
     *
     * The reader task only read the tnf NdefRecord.TNF_WELL_KNOWN
     * with the type of NdefRecord.RTD_TEXT
     */
    private class NdefReaderTask extends AsyncTask<Tag, Void, String> {

        @Override
        protected String doInBackground(Tag... params) {
            Tag tag = params[0];

            Ndef ndef = Ndef.get(tag);
            if (ndef == null) {
                // NDEF is not supported by this Tag.
                updateLog("ndef=null, NDEF is not supported by this Tag.");
                return null;
            }

            NdefMessage ndefMessage = ndef.getCachedNdefMessage();

            NdefRecord[] records = ndefMessage.getRecords();
            for (NdefRecord ndefRecord : records) {
                Log.i(TAG, "ndefRecord.getTnf() = " + ndefRecord.getTnf() + ", ndefRecord.getType()=" + ndefRecord.getType());
                if (ndefRecord.getTnf() == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(ndefRecord.getType(), NdefRecord.RTD_TEXT)) {
                    try {
                        return readText(ndefRecord);
                    } catch (UnsupportedEncodingException e) {
                        Log.e(TAG, "Unsupported Encoding", e);
                    }
                }
            }

            return null;
        }

        private String readText(NdefRecord record) throws UnsupportedEncodingException {
        /*
         * See NFC forum specification for "Text Record Type Definition" at 3.2.1
         *
         * http://www.nfc-forum.org/specs/
         *
         * bit_7 defines encoding
         * bit_6 reserved for future use, must be 0
         * bit_5..0 length of IANA language code
         */

            byte[] payload = record.getPayload();

            // Get the Text Encoding
            String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";

            // Get the Language Code
            int languageCodeLength = payload[0] & 0063;

            // String languageCode = new String(payload, 1, languageCodeLength, "US-ASCII");
            // e.g. "en"

            // Get the Text
            return new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                updateLog("Read content: " + result);
            }
        }
    }


    /**
     * NdefRecord is set to NdefRecord.TNF_WELL_KNOWN and type NdefRecord.RTD_TEXT
     * @param msg The "msg" you want to write inside to the NFC tag
     * @throws UnsupportedEncodingException
     */
    private NdefRecord createRecord(String msg) throws UnsupportedEncodingException {
        //create the message in according with the standard
        String lang = "en";
        byte[] textBytes = msg.getBytes();
        byte[] langBytes = lang.getBytes("US-ASCII");
        int langLength = langBytes.length;
        int textLength = textBytes.length;

        byte[] payload = new byte[1 + langLength + textLength];
        payload[0] = (byte) langLength;

        // copy langbytes and textbytes into payload
        System.arraycopy(langBytes, 0, payload, 1, langLength);
        System.arraycopy(textBytes, 0, payload, 1 + langLength, textLength);

        NdefRecord recordNFC = new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], payload);
        return recordNFC;
    }


    public boolean writeTag(Context context, Tag tag, String msg) {
        try {
            NdefMessage message = new NdefMessage(new NdefRecord[]{createRecord(msg)});
            try {
                // If the tag is already formatted, just write the message to it
                Ndef ndef = Ndef.get(tag);
                if(ndef != null) {
                    ndef.connect();

                    // Make sure the tag is writable
                    if(!ndef.isWritable()) {
                        updateLog("Tag is writable");
                        return false;
                    }

                    // Check if there's enough space on the tag for the message
                    int msgSize = message.toByteArray().length;
                    if(ndef.getMaxSize() < msgSize) {
                        updateLog("not enough space on the tag for the message");
                        return false;
                    }

                    try {
                        // Write the data to the tag
                        ndef.writeNdefMessage(message);
                        updateLog("write the message to the tag");
                        return true;
                    } catch (TagLostException tle) {
                        updateLog("TagLostException " + tle.toString());
                        return false;
                    } catch (IOException ioe) {
                        updateLog("IOException " + ioe.toString());
                        return false;
                    } catch (FormatException fe) {
                        updateLog("FormatException " + fe.toString());
                        return false;
                    }
                    // If the tag is not formatted, format it with the message
                } else {
                    NdefFormatable format = NdefFormatable.get(tag);
                    if(format != null) {
                        try {
                            format.connect();
                            format.format(message);
                            updateLog("tag is not formatted, format it with message");
                            return true;
                        } catch (TagLostException tle) {
                            updateLog("TagLostException " + tle.toString());
                            return false;
                        } catch (IOException ioe) {
                            updateLog("IOException " + ioe.toString());
                            return false;
                        } catch (FormatException fe) {
                            updateLog("FormatException " + fe.toString());
                            return false;
                        }
                    } else {
                        updateLog("tag format fail");
                        return false;
                    }
                }
            } catch(Exception e) {
                updateLog("Exception " + e.toString());
            }
        } catch(Exception e) {
            updateLog("Create NdefMessage Exception: " + e);
        }
        return false;
    }
}
