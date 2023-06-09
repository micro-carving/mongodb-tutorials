package com.olinonee.mongodb.quickstart.test;

import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertManyResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import org.bson.BsonValue;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;

/**
 * crud 测试
 * <p>
 * 参考：<a href="https://learn.mongodb.com/learn/course/mongodb-crud-operations-in-java">mongodb-crud-operations-in-java</a>
 *
 * @author olinH, olinone666@gmail.com
 * @version v1.0.0
 * @since 2023-04-28
 */
public class CrudTests {

    // 格式：[jdbc:]mongodb[+srv]://[{user:identifier}[:{password:param}]@]<\,,{host::localhost}?[:{port::27017}]>[/{database}?[\?<&,{:identifier}={:param}>]]
    private static final String connectString = "mongodb://root:root@localhost:27017";
    // private static final String txConnectString = "mongodb://root:root@localhost:27017/admin?retryWrites=false";
    private static final String txConnectString = "mongodb://localhost:27020/retryWrites=false";

    @Test
    void testInsertOne() {
        try (MongoClient mongoClient = MongoClients.create(connectString)) {
            final MongoDatabase database = mongoClient.getDatabase("sample_training");
            final MongoCollection<Document> collection = database.getCollection("inspections");

            final Document inspection = new Document("_id", new ObjectId())
                    .append("id", "10021-2015-ENFO")
                    .append("certificate_number", 9278806)
                    .append("business_name", "ATLIXCO DELI GROCERY INC.")
                    .append("date", Date.from(LocalDate.of(2015, 2, 20).atStartOfDay(ZoneId.systemDefault()).toInstant()))
                    .append("result", "No Violation Issued")
                    .append("sector", "Cigarette Retail Dealer - 127")
                    .append("address", new Document().append("city", "RIDGEWOOD").append("zip", 11385).append("street", "MENAHAN ST").append("number", 1712));
            final InsertOneResult insertOneResult = collection.insertOne(inspection);
            final BsonValue insertedId = insertOneResult.getInsertedId();
            System.out.println(insertedId);
        }
    }

    @Test
    void testInsertMany() {
        try (MongoClient mongoClient = MongoClients.create(connectString)) {
            final MongoDatabase database = mongoClient.getDatabase("bank");
            final MongoCollection<Document> collection = database.getCollection("accounts");

            final Document doc1 = new Document().append("account_holder", "john doe").append("account_id", "MDB99115881").append("balance", 1785).append("account_type", "checking");
            final Document doc2 = new Document().append("account_holder", "jane doe").append("account_id", "MDB79101843").append("balance", 1468).append("account_type", "checking");

            final InsertManyResult insertManyResult = collection.insertMany(Arrays.asList(doc1, doc2));
            insertManyResult.getInsertedIds().forEach((k, v) -> System.out.println(k + " -> " + v.asObjectId()));
        }
    }

    @Test
    void testFind() {
        try (MongoClient mongoClient = MongoClients.create(connectString)) {
            MongoDatabase database = mongoClient.getDatabase("bank");
            MongoCollection<Document> collection = database.getCollection("accounts");
            try (MongoCursor<Document> cursor = collection.find(Filters.and(Filters.gte("balance", 1000), Filters.eq("account_type", "checking")))
                    .iterator()) {
                while (cursor.hasNext()) {
                    System.out.println(cursor.next().toJson());
                }
            }
        }
    }

    @Test
    void testFindFirst() {
        try (MongoClient mongoClient = MongoClients.create(connectString)) {
            MongoDatabase database = mongoClient.getDatabase("bank");
            MongoCollection<Document> collection = database.getCollection("accounts");
            final Document document = collection.find(Filters.and(Filters.gte("balance", 1000), Filters.eq("account_type", "checking"))).first();
            assert document != null;
            System.out.println(document.toJson());
        }
    }

    @Test
    void testUpdateOne() {
        try (MongoClient mongoClient = MongoClients.create(connectString)) {
            MongoDatabase database = mongoClient.getDatabase("bank");
            MongoCollection<Document> collection = database.getCollection("accounts");
            Bson query = Filters.eq("account_id", "MDB79101843");
            Bson updates = Updates.combine(Updates.set("account_status", "active"), Updates.inc("balance", 100));
            UpdateResult upResult = collection.updateOne(query, updates);
            System.out.println(upResult);
        }
    }

    @Test
    void testUpdateMany() {
        try (MongoClient mongoClient = MongoClients.create(connectString)) {
            MongoDatabase database = mongoClient.getDatabase("bank");
            MongoCollection<Document> collection = database.getCollection("accounts");
            Bson query = Filters.eq("account_type", "checking");
            Bson updates = Updates.combine(Updates.set("minimum_balance", 100));
            UpdateResult upResult = collection.updateMany(query, updates);
            System.out.println(upResult);
        }
    }

    @Test
    void testDeleteOne() {
        try (MongoClient mongoClient = MongoClients.create(connectString)) {
            MongoDatabase database = mongoClient.getDatabase("bank");
            MongoCollection<Document> collection = database.getCollection("accounts");
            Bson query = Filters.eq("account_holder", "john doe");
            DeleteResult delResult = collection.deleteOne(query);
            System.out.println("Deleted a document:\t" + delResult.getDeletedCount());
        }
    }

    @Test
    void testDeleteManyWithQueryObject() {
        try (MongoClient mongoClient = MongoClients.create(connectString)) {
            MongoDatabase database = mongoClient.getDatabase("bank");
            MongoCollection<Document> collection = database.getCollection("accounts_doc_template");
            Bson query = Filters.eq("state", "TN");
            DeleteResult delResult = collection.deleteMany(query);
            System.out.println("Deleted document‘s counts are:\t" + delResult.getDeletedCount());
        }
    }

    @Test
    void testDeleteManyWithQueryFilter() {
        try (MongoClient mongoClient = MongoClients.create(connectString)) {
            MongoDatabase database = mongoClient.getDatabase("bank");
            MongoCollection<Document> collection = database.getCollection("accounts_doc_template");
            DeleteResult delResult = collection.deleteMany(Filters.eq("state", "VA"));
            System.out.println("Deleted document‘s counts are:\t" + delResult.getDeletedCount());
        }
    }

    @Test
    void testTransaction() {
        try (MongoClient mongoClient = MongoClients.create(txConnectString)) {
            final ClientSession clientSession = mongoClient.startSession();

            final TransactionBody<String> transactionBody = () -> {
                MongoCollection<Document> bankingCollection = mongoClient.getDatabase("bank").getCollection("accounts_test");

                // 提取
                Bson fromAccount = Filters.eq("account_id", "MDB310054629");
                Bson withdrawal = Updates.inc("balance", -200);

                // 存入
                Bson toAccount = Filters.eq("account_id", "MDB643731035");
                Bson deposit = Updates.inc("balance", 200);

                System.out.println("This is from Account " + fromAccount.toBsonDocument().toJson() + " withdrawn " + withdrawal.toBsonDocument().toJson());
                System.out.println("This is to Account " + toAccount.toBsonDocument().toJson() + " deposited " + deposit.toBsonDocument().toJson());
                bankingCollection.updateOne(clientSession, fromAccount, withdrawal);
                bankingCollection.updateOne(clientSession, toAccount, deposit);

                return "Transferred funds from John Doe to Mary Doe";
            };

            try {
                // 开启事务
                clientSession.withTransaction(transactionBody);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                clientSession.close();
            }
        }
    }
}
