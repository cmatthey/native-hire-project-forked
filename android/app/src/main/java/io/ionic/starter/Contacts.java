package io.ionic.starter;

import android.Manifest;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.ContactsContract;


import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.NativePlugin;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;

import java.util.Arrays;

@NativePlugin(
    requestCodes = { Contacts.GET_ALL_REQUEST }
)
public class Contacts extends Plugin {
  static final int GET_ALL_REQUEST = 30033;

  // Part 1: Add the functionality to retrieve all the contacts from the device on Android.
  @PluginMethod()
  public void getAllPart1(PluginCall call) {
    if (!hasPermission(Manifest.permission.READ_CONTACTS) || !hasPermission(Manifest.permission.WRITE_CONTACTS)) {
      saveCall(call);
      pluginRequestPermissions(new String[] { Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS }, GET_ALL_REQUEST);
      return;
    }

    JSObject result = new JSObject();
    JSArray contacts = this.getContactsHelper(null);
    result.put("contacts", contacts);
    call.success(result);
  }

  // Part 2: Add a method to the contacts plugin that allows for querying contacts on the device.
    @PluginMethod()
    public void getAll(PluginCall call) { //TODO: register a capacitor plugin
      String keyword = call.hasOption("keyword") ? call.getString("keyword") : "";

      if (!hasPermission(Manifest.permission.READ_CONTACTS) || !hasPermission(Manifest.permission.WRITE_CONTACTS)) {
        saveCall(call);
        pluginRequestPermissions(new String[] { Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS }, GET_ALL_REQUEST);
        return;
      }

      JSObject result = new JSObject();
      JSArray contacts = this.getContactsHelper(keyword);
      result.put("contacts", contacts);
      call.success(result);
    }

    protected JSArray getContactsHelper(String keyword) {
      JSArray contacts = new JSArray();
      ContentResolver cr = getActivity().getContentResolver();
      String SELECTION = ContactsContract.Contacts.DISPLAY_NAME_PRIMARY + " LIKE ?";
      Cursor cur = null;
      if (keyword == null || keyword.isEmpty()) {
        cur = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
      } else {
        cur = cr.query(ContactsContract.Contacts.CONTENT_URI, null, SELECTION, new String[] {"%"+keyword+"%"}, null);
      }
      if ((cur != null ? cur.getCount() : 0) > 0) {
        while (cur != null && cur.moveToNext()) {
          String id = cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID));
          String firstName = "";
          String lastName = "";
          JSArray phoneNumbers =  new JSArray();
          JSArray emailAddresses =  new JSArray();
          String displayName = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY));
          Cursor nCur = cr.query(ContactsContract.Data.CONTENT_URI, null,
                  ContactsContract.Data.MIMETYPE + " = ? AND " + ContactsContract.CommonDataKinds.StructuredName.CONTACT_ID + " = ?", new String[] { ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE, id}, null);
          while (nCur.moveToNext()) {
            firstName = nCur.getString(nCur.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME));
            lastName = nCur.getString(nCur.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME));
          }
          nCur.close();
          // Get phone numbers
          if (cur.getInt(cur.getColumnIndex( ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0) {
            Cursor pCur = cr.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null,
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                    new String[]{id}, null);
            while (pCur.moveToNext()) {
              String phoneNo = pCur.getString(pCur.getColumnIndex(
                      ContactsContract.CommonDataKinds.Phone.NUMBER));
              phoneNumbers.put(phoneNo);
            }
            pCur.close();
          }
          // Get emails
          Cursor eCur = cr.query(
                  ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                  null,
                  ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
                  new String[]{id}, null);
          while (eCur.moveToNext()) {
            String emailAddr = eCur.getString(eCur.getColumnIndex(
                    ContactsContract.CommonDataKinds.Email.ADDRESS));
            emailAddresses.put(emailAddr);
          }
          eCur.close();

          JSObject cont = new JSObject();
          cont.put("id", id);
          cont.put("firstName", firstName);
          cont.put("lastName", lastName);
          cont.put("phoneNumbers", phoneNumbers);
          cont.put("emailAddresses", emailAddresses);
          contacts.put(cont);
        }
      }
      if (cur != null) {
        cur.close();
      }
      return contacts;
    }

  @Deprecated
  protected JSArray getAllMocked() {
    JSArray contacts = new JSArray();
    JSObject eltonJson = new JSObject();
    eltonJson.put("firstName", "Elton");
    eltonJson.put("lastName", "Json");
    eltonJson.put("phoneNumbers", new JSArray(Arrays.asList("2135551111")));
    eltonJson.put("emailAddresses", new JSArray(Arrays.asList("elton@eltonjohn.com")));
    contacts.put(eltonJson);
    JSObject freddieMercury = new JSObject();
    freddieMercury.put("firstName", "Freddie");
    freddieMercury.put("lastName", "Mercury");
    freddieMercury.put("phoneNumbers", new JSArray());
    freddieMercury.put("emailAddresses", new JSArray());
    contacts.put(freddieMercury);

    return contacts;
  }

  @Override
  protected void handleRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    super.handleRequestPermissionsResult(requestCode, permissions, grantResults);

    PluginCall savedCall = getSavedCall();
    if (savedCall == null) {
      return;
    }

    for(int result : grantResults) {
      if (result == PackageManager.PERMISSION_DENIED) {
        savedCall.error("User denied permission");
        return;
      }
    }

    if (requestCode == GET_ALL_REQUEST) {
      this.getAll(savedCall);
    }
  }
}
