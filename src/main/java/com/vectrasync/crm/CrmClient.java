package com.vectrasync.crm;

import java.util.List;
import java.util.Optional;

public interface CrmClient {

    String name();

    List<FieldSchema> getSchema();

    Optional<Contact> findContact(String email);

    UpsertResult upsertContact(Contact contact);
}
