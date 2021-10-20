package de.koudingspawn.vault;

import org.json.JSONObject;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class TestHelper {

    public static void generateLookupSelfStub() {
        stubFor(get(urlEqualTo("/v1/auth/token/lookup-self"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\n" +
                                "    \"request_id\": \"200ef4ee-7ca7-9d38-2e63-6002454e00d7\",\n" +
                                "    \"lease_id\": \"\",\n" +
                                "    \"renewable\": false,\n" +
                                "    \"lease_duration\": 0,\n" +
                                "    \"data\": {\n" +
                                "        \"accessor\": \"c69c3bd7-c142-c655-2757-77bfdc86b04a\",\n" +
                                "        \"creation_time\": 1536033750,\n" +
                                "        \"creation_ttl\": 0,\n" +
                                "        \"display_name\": \"root\",\n" +
                                "        \"entity_id\": \"\",\n" +
                                "        \"expire_time\": null,\n" +
                                "        \"explicit_max_ttl\": 0,\n" +
                                "        \"id\": \"c73ab0cb-41e6-b89c-7af6-96b36f1ac87b\",\n" +
                                "        \"meta\": null,\n" +
                                "        \"num_uses\": 0,\n" +
                                "        \"orphan\": true,\n" +
                                "        \"path\": \"auth/token/root\",\n" +
                                "        \"policies\": [\n" +
                                "            \"root\"\n" +
                                "        ],\n" +
                                "        \"ttl\": 0\n" +
                                "    },\n" +
                                "    \"wrap_info\": null,\n" +
                                "    \"warnings\": null,\n" +
                                "    \"auth\": null\n" +
                                "}")));
    }

    public static void generateKVStup(String path, Map<String, String> value) {
        JSONObject jsonObject = new JSONObject(value);


        stubFor(get(urlPathMatching("/v1/" + path))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"request_id\":\"6cc090a8-3821-8244-73e4-5ab62b605587\",\"lease_id\":\"\",\"renewable\":false,\"lease_duration\":2764800,\"data\":" + jsonObject.toString() + ",\"wrap_info\":null,\"warnings\":null,\"auth\":null}")));

    }

    public static void generateKV2Stup(String path, Map<String, String> value) {
        JSONObject jsonObject = new JSONObject(value);
        String[] splittedPath = path.split("/");

        stubFor(get(urlPathMatching("/v1/" + splittedPath[0] + "/data/" + splittedPath[1]))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\n" +
                                "  \"request_id\": \"1cfee2a6-318a-ea12-f5b5-6fd52d74d2c6\",\n" +
                                "  \"lease_id\": \"\",\n" +
                                "  \"renewable\": false,\n" +
                                "  \"lease_duration\": 0,\n" +
                                "  \"data\": {\n" +
                                "    \"data\": " + jsonObject.toString() + ",\n" +
                                "    \"metadata\": {\n" +
                                "      \"created_time\": \"2018-12-10T18:59:53.337997525Z\",\n" +
                                "      \"deletion_time\": \"\",\n" +
                                "      \"destroyed\": false,\n" +
                                "      \"version\": 1\n" +
                                "    }\n" +
                                "  },\n" +
                                "  \"wrap_info\": null,\n" +
                                "  \"warnings\": null,\n" +
                                "  \"auth\": null\n" +
                                "}")));
    }

}
