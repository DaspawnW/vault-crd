package de.koudingspawn.vault;

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

}
