package xyz.heart.sms.api;

import org.junit.Test;

import java.io.IOException;

import xyz.heart.sms.api.entity.AddContactRequest;
import xyz.heart.sms.api.entity.ContactBody;
import xyz.heart.sms.api.entity.UpdateContactRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ContactTest extends ApiTest {
    @Test
    public void addAndUpdateAndRemove() throws IOException {
        String accountId = getAccountId();
        int originalSize = api.contact().list(accountId).execute().body().length;

        //TODO. This method was called without the id, and wouldn't compile, so I just added the 0L to be able to build the app.
        ContactBody contact = new ContactBody(0L, "515", "515", "Luke", 1, 1, 1, 1);
        AddContactRequest request = new AddContactRequest(accountId, contact);
        Object response = api.contact().add(request).execute().body();
        assertNotNull(response);

        UpdateContactRequest update = new UpdateContactRequest(null, "jake", null, null, null, null);
        api.contact().update("515", 1, accountId, update).execute().body();

        ContactBody[] contacts = api.contact().list(accountId).execute().body();
        assertEquals(1, contacts.length - originalSize);

        api.contact().remove("515", 1, accountId).execute();

        contacts = api.contact().list(accountId).execute().body();
        assertEquals(contacts.length, originalSize);
    }

}
