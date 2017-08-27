package krishna.example.com.conmap;

import android.*;
import android.Manifest;
import android.app.Dialog;
import android.content.ContentProviderOperation;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback,GoogleMap.OnMarkerClickListener{

    private Button addButton;
    private int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 5,networkstatus = 0;
    private SupportMapFragment mapFragment;
    private String lat[] = new String[5],lon[] = new String[5],name[] = new String[5];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Initialize views
        addButton = (Button)findViewById(R.id.add_Button);

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                networkThread thread = new networkThread();
                thread.start();
                try {
                    thread.join();
                    if(networkstatus==0){
                        Toast.makeText(MainActivity.this,"Please enable Internet",Toast.LENGTH_SHORT).show();
                        return;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mapFragment = (SupportMapFragment)getSupportFragmentManager().findFragmentById(R.id.map_fragment);
                mapFragment.getMapAsync(MainActivity.this);
                v.setClickable(false);
            }
        });

        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_CONTACTS)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                Toast.makeText(this, "Contacts Permisson needed!!!", Toast.LENGTH_SHORT).show();
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_CONTACTS},
                        MY_PERMISSIONS_REQUEST_READ_CONTACTS);
            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_CONTACTS},
                        MY_PERMISSIONS_REQUEST_READ_CONTACTS);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }

    }


    private String readFromURL(String url){
        StringBuilder data = new StringBuilder();
        try {
            URL link = new URL(url);
            //URLConnection urlConnection = link.openConnection();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(link.openStream()));
            if(bufferedReader==null){
                networkstatus = 0;
                return null;
            }
            networkstatus = 1;
            String line;
            while((line = bufferedReader.readLine())!=null){
                data.append(line+"\n");
            }
            bufferedReader.close();
            return data.toString();
        } catch (Exception e) {
            e.printStackTrace();
            //Toast.makeText(this,"Error reading URL",Toast.LENGTH_LONG).show();
            return null;
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        for(int i=0;i<lat.length;i++){
            LatLng loc = new LatLng(Integer.parseInt(lat[i])/1000000,Integer.parseInt(lon[i])/1000000);
            googleMap.addMarker(new MarkerOptions().position(loc)
                    .title(name[i]));
        }
        googleMap.setOnMarkerClickListener(MainActivity.this);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this,android.R.style.Theme_Material_Dialog_Alert);
        builder.setMessage(marker.getTitle());
        builder.show();
        return true;
    }


    private class networkThread extends Thread {
        @Override
        public void run() {
            //get data from given url
            String dataString = readFromURL("http://www.cs.columbia.edu/~coms6998-8/assignments/homework2/contacts/contacts.txt");

            if(dataString!=null){

                String[] dataLines = dataString.split("\n");
                for(int i = 0;i < dataLines.length;i++){
                    String[] dataItems = dataLines[i].split(" ");

                    lat[i] = dataItems[2];
                    lon[i] = dataItems[3];
                    name[i] = dataItems[0];

                    ArrayList<ContentProviderOperation> arrayList = new ArrayList<>();
                    arrayList.add(ContentProviderOperation.newInsert(
                            ContactsContract.RawContacts.CONTENT_URI)
                            .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                            .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                            .build());

                    //------------------------------------------------------ Names
                    if (dataItems[0] != null) {
                        arrayList.add(ContentProviderOperation.newInsert(
                                ContactsContract.Data.CONTENT_URI)
                                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                                .withValue(ContactsContract.Data.MIMETYPE,
                                        ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                                .withValue(
                                        ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME,
                                        dataItems[0]).build());
                    }

                    //------------------------------------------------------ Mobile Number
                    if (dataItems[2] != null) {
                        arrayList.add(ContentProviderOperation.
                                newInsert(ContactsContract.Data.CONTENT_URI)
                                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                                .withValue(ContactsContract.Data.MIMETYPE,
                                        ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, dataItems[2])
                                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE,
                                        ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                                .build());
                    }

                    //------------------------------------------------------ Home Numbers
                    if (dataItems[3] != null) {
                        arrayList.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                                .withValue(ContactsContract.Data.MIMETYPE,
                                        ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, dataItems[3])
                                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE,
                                        ContactsContract.CommonDataKinds.Phone.TYPE_HOME)
                                .build());
                    }

                    //------------------------------------------------------ Email
                    if (dataItems[1] != null) {
                        arrayList.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                                .withValue(ContactsContract.Data.MIMETYPE,
                                        ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                                .withValue(ContactsContract.CommonDataKinds.Email.DATA, dataItems[1])
                                .withValue(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_WORK)
                                .build());
                    }
                    try {
                        getContentResolver().applyBatch(ContactsContract.AUTHORITY, arrayList);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            }

        }
    }
}
