package com.indeed.skeleton.index.builder.jiraaction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.indeed.common.util.StringUtils;
import org.apache.commons.codec.binary.Base64;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;

/**
 * @author soono
 */
public class IssuesAPICaller {
    private final JiraActionIndexBuilderConfig config;
    //
    // For Pagination
    //

    private final int numPerPage = 10; // Max number of issues per page
    private int page = 0; // Current Page
    private int numTotal = -1; // Max number of issues per page

    public IssuesAPICaller(final JiraActionIndexBuilderConfig config) {
        this.config = config;
    }

    public JsonNode getIssuesNode() throws IOException {
        final JsonNode apiRes = getJsonNode(getIssuesURL());
        setNextPage();
        return apiRes.get("issues");
    }

    //
    // Call API with URL and parse response to JSON node.
    //

    private JsonNode getJsonNode(final String url) throws IOException {
        final HttpsURLConnection urlConnection = getURLConnection(url);
        final InputStream in = urlConnection.getInputStream();
        final BufferedReader br = new BufferedReader(new InputStreamReader(in));
        final String apiRes = br.readLine();
        final ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(apiRes);
    }

    //
    // For Pagination
    //

    public int setNumTotal() throws IOException {
        final JsonNode apiRes = getJsonNode(getBasicInfoURL());
        final JsonNode totalNode = apiRes.path("total");
        this.numTotal = totalNode.intValue();
        return numTotal;
    }

    public boolean currentPageExist() {
        return (page * numPerPage) < numTotal;
    }

    private int setNextPage() {
            return ++page;
    }

    private int getStartAt() {
        // startAt starts from 0
        return page * numPerPage;
    }


    //
    // For Getting URL Connection
    //

    private HttpsURLConnection getURLConnection(final String urlString) throws IOException {
        System.out.println(urlString);
        final URL url = new URL(urlString);
        final HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
        urlConnection.setRequestProperty("Authorization", getBasicAuth());
        return urlConnection;
    }

    private String getBasicAuth() {
        final String userPass = config.getJiraUsernameIndexer() + ":" + config.getJiraPasswordIndexer();
        final String basicAuth = "Basic " + new String(new Base64().encode(userPass.getBytes()));
        return basicAuth;
    }

    private String getIssuesURL() throws UnsupportedEncodingException {
        final StringBuilder url = new StringBuilder(config.getJiraBaseURL() + "?")
                .append(getJQLParam())
                .append("&")
                .append(getFieldsParam())
                .append("&")
                .append(getExpandParam())
                .append("&")
                .append(getStartAtParam())
                .append("&")
                .append(getMaxResults());
        return url.toString();
    }

    private String getBasicInfoURL() throws UnsupportedEncodingException {
        final StringBuilder url = new StringBuilder(config.getJiraBaseURL() + "?")
                .append(getJQLParam())
                .append("&maxResults=0");
        return url.toString();
    }

    private String getJQLParam() throws UnsupportedEncodingException {
        final StringBuilder query = new StringBuilder();
        query.append("updatedDate>=").append(config.getStartDate())
                .append(" AND updatedDate<").append(config.getEndDate());

        if(!StringUtils.isEmpty(config.getJiraProject())) {
            query.append(" AND project IN (").append(config.getJiraProject()).append(")");
        }

        return "jql=" + URLEncoder.encode(query.toString(), "UTF-8");
    }

    private String getFieldsParam() {
        return String.format("fields=%s", config.getJiraFields());
    }

    private String getExpandParam() {
        return String.format("expand=%s", config.getJiraExpand());
    }

    private String getStartAtParam() {
        return String.format("startAt=%d", getStartAt());
    }

    private String getMaxResults() {
        return String.format("maxResults=%d", numPerPage);
    }
}