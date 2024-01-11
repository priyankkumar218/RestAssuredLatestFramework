package com.qa.api.tests;

import com.github.javafaker.Faker;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import lombok.Data;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class GoBankAPITests {
    private static String jwtToken;

    private static int lastAccountNumber;

    private static int accountId;

    private Faker faker = new Faker();

    @BeforeMethod
    public void setUp() {
        RestAssured.baseURI = "http://localhost:3000";
    }

    @Test(priority = 0)
    public void testCreateAccount() {
        String firstName = faker.name().firstName();
        String lastName = faker.name().lastName();
        String password = "root";

        AccountRequest accountRequest = new AccountRequest(firstName, lastName, password);

        given()
                .contentType(ContentType.JSON)
                .body(accountRequest)
                .when()
                .post("/account")
                .then()
                .assertThat()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("firstName", equalTo(firstName))
                .body("lastName", equalTo(lastName))
                .body("balance", equalTo(0))
                .body("createdAt", notNullValue());
    }

    @Data
    public static class AccountRequest {
        private String firstName;
        private String lastName;
        private String password;

        public AccountRequest(String firstName, String lastName, String password) {
            this.firstName = firstName;
            this.lastName = lastName;
            this.password = password;
        }
    }

    @Test(priority = 1)
    public void testGetAccounts() {
        String response = given()
                .contentType(ContentType.JSON)
                .when()
                .get("/account")
                .then()
                .assertThat()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("$.size()", greaterThan(0))
                .body("id", hasItems(2))
                .body("firstName", notNullValue())
                .body("lastName", notNullValue())
                .body("number", notNullValue())
                .body("balance", notNullValue())
                .body("createdAt", notNullValue())
                .extract()
                .asString();

        // accountNumber
        int lastIndex = response.lastIndexOf("\"number\":");
        int endOfNumberIndex = response.indexOf(",", lastIndex);
        String numberSubstring = response.substring(lastIndex + 9, endOfNumberIndex).trim();
        lastAccountNumber = Integer.parseInt(numberSubstring);

        // Id
        int lastIndexId = response.lastIndexOf("\"id\":");
        int endOfIdIndex = response.indexOf(",", lastIndexId);
        String idSubstring = response.substring(lastIndexId + 5, endOfIdIndex).trim();
        accountId = Integer.parseInt(idSubstring);
    }

    @Test(priority = 2)
    public void testTransferAmount() {
        TransferRequest transferRequest = new TransferRequest(lastAccountNumber, 400000);

        given()
                .contentType(ContentType.JSON)
                .body(transferRequest)
                .when()
                .patch("/transfer")
                .then()
                .assertThat()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("toAccount", equalTo(lastAccountNumber))
                .body("amount", equalTo(400000));
    }

    @Data
    public static class TransferRequest {
        private int toAccount;
        private int amount;

        public TransferRequest(int toAccount, int amount) {
            this.toAccount = toAccount;
            this.amount = amount;
        }
    }

    @Test(priority = 3)
    public void testLoginAndGetToken() {
        LoginRequest loginRequest = new LoginRequest(lastAccountNumber, "root");

        jwtToken = given()
                .contentType(ContentType.JSON)
                .body(loginRequest)
                .when()
                .post("/login")
                .then()
                .assertThat()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .path("token");
    }

    @Data
    public static class LoginRequest {
        private Integer number;
        private String password;

        public LoginRequest(Integer number, String password) {
            this.number = number;
            this.password = password;
        }
    }

    @Test(priority = 4)
    public void testGetAccountWithToken() {
        given()
                .header("x-jwt-token", jwtToken)
                .contentType(ContentType.JSON)
                .when()
                .get("/account/"+accountId)
                .then()
                .assertThat()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("id", equalTo(accountId))
                .log().all();
    }
}
