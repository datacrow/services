/**********************************************************************************************
 * Copyright 2009 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file 
 * except in compliance with the License. A copy of the License is located at
 *
 *       http://aws.amazon.com/apache2.0/
 *
 * or in the "LICENSE.txt" file accompanying this file. This file is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the License. 
 *
 * ********************************************************************************************
 *
 *  Amazon Product Advertising API
 *  Signed Requests Sample Code
 *
 *  API Version: 2009-03-31
 *
 */

package net.datacrow.onlinesearch.amazon.server;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import net.datacrow.util.Base64;

/**
 * This class contains all the logic for signing requests
 * to the Amazon Product Advertising API.
 */
public class SignedRequestsHelper {
    
    private static final String UTF8_CHARSET = "UTF-8";
    private static final String HMAC_SHA256_ALGORITHM = "HmacSHA256";
    private static final String REQUEST_URI = "/onca/xml";
    private static final String REQUEST_METHOD = "GET";

    private final String awsAccessKeyId;
    private final String awsSecretKey;
    private final SecretKeySpec secretKeySpec;
    private final Mac mac;

    public SignedRequestsHelper(String awsAccessKeyId, String awsSecretKey) throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException {
        this.awsAccessKeyId = awsAccessKeyId;
        this.awsSecretKey = awsSecretKey;

        byte[] secretyKeyBytes = this.awsSecretKey.getBytes(UTF8_CHARSET);
        this.secretKeySpec = new SecretKeySpec(secretyKeyBytes, HMAC_SHA256_ALGORITHM);
        this.mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
        this.mac.init(secretKeySpec);
    }
    
    
    public synchronized String sign(URL url) {
        String server = url.getHost();
        String query = url.getQuery();
        
        Map<String, String> params = createParameterMap(query);
        
        // remove old signature information
        params.remove("AWSAccessKeyId");
        params.remove("Timestamp");
        params.remove("Signature");
        
        return sign(server, params);
    }
    
    /**
     * This method signs requests in hash map form. It returns a URL that should
     * be used to fetch the response. The URL returned should not be modified in
     * any way, doing so will invalidate the signature and Amazon will reject
     * the request.
     */
    public synchronized String sign(String server, Map<String, String> params) {
        params.put("AWSAccessKeyId", this.awsAccessKeyId);
        params.put("Timestamp", this.timestamp());

        SortedMap<String, String> sortedParamMap = new TreeMap<String, String>(params);
        String canonicalQS = this.canonicalize(sortedParamMap);
        
        // create the string upon which the signature is calculated 
        String toSign = REQUEST_METHOD + "\n" + server + "\n"+ 
                        REQUEST_URI + "\n" + canonicalQS;

        // sign
        String hmac = this.hmac(toSign);
        String sig = this.percentEncodeRfc3986(hmac);

        return "http://" + server + REQUEST_URI + "?" + canonicalQS + "&Signature=" + sig;
    }

    /**
     * This method signs requests in query-string form. It returns a URL that
     * should be used to fetch the response. The URL returned should not be
     * modified in any way, doing so will invalidate the signature and Amazon
     * will reject the request.
     */
    public String sign(String server, String queryString) {
        Map<String, String> params = this.createParameterMap(queryString);
        return this.sign(server, params);
    }

    /**
     * Compute the HMAC.
     *  
     * @param stringToSign  String to compute the HMAC over.
     * @return              base64-encoded HMAC value.
     */
    private String hmac(String stringToSign) {
        String signature = null;
        byte[] data;
        byte[] rawHmac;
        try {
            data = stringToSign.getBytes(UTF8_CHARSET);
            rawHmac = mac.doFinal(data);
            signature = new String(Base64.encode(rawHmac));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(UTF8_CHARSET + " is unsupported!", e);
        }
        return signature;
    }

    /**
     * Generate a ISO-8601 format time stamp as required by Amazon.
     * @return  ISO-8601 format time stamp.
     */
    private String timestamp() {
        String timestamp = null;
        Calendar cal = Calendar.getInstance();
        DateFormat dfm = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        dfm.setTimeZone(TimeZone.getTimeZone("GMT"));
        timestamp = dfm.format(cal.getTime());
        return timestamp;
    }

    /**
     * Canonicalize the query string as required by Amazon.
     * 
     * @param sortedParamMap    Parameter name-value pairs in lexicographical order.
     * @return                  Canonical form of query string.
     */
    private String canonicalize(SortedMap<String, String> sortedParamMap) {
        if (sortedParamMap.isEmpty()) {
            return "";
        }

        StringBuffer buffer = new StringBuffer();
        Iterator<Map.Entry<String, String>> iter = sortedParamMap.entrySet().iterator();

        while (iter.hasNext()) {
            Map.Entry<String, String> kvpair = iter.next();
            buffer.append(percentEncodeRfc3986(kvpair.getKey()));
            buffer.append("=");
            buffer.append(percentEncodeRfc3986(kvpair.getValue()));
            if (iter.hasNext()) {
                buffer.append("&");
            }
        }
        String cannoical = buffer.toString();
        return cannoical;
    }

    /**
     * Percent-encode values according the RFC 3986. The built-in Java
     * URLEncoder does not encode according to the RFC, so we make the
     * extra replacements.
     * 
     * @param s decoded string
     * @return  encoded string per RFC 3986
     */
    private String percentEncodeRfc3986(String s) {
        String out;
        try {
            out = URLEncoder.encode(s, UTF8_CHARSET).replace("+", "%20").replace("*", "%2A").replace("%7E", "~");
        } catch (UnsupportedEncodingException e) {
            out = s;
        }
        return out;
    }

    /**
     * Takes a query string, separates the constituent name-value pairs
     * and stores them in a hash map.
     * 
     * @param queryString
     * @return
     */
    private Map<String, String> createParameterMap(String queryString) {
        Map<String, String> map = new HashMap<String, String>();
        
        queryString = queryString.indexOf("&Signature") > -1 ? queryString.substring(0, queryString.indexOf("&Signature")) : queryString;
        String[] pairs = queryString.split("&");

        for (String pair: pairs) {
            
            if (pair.length() < 1) continue;

            String[] tokens = pair.split("=", 2);
            for(int j = 0; j < tokens.length; j++) {
                try {
                    tokens[j] = URLDecoder.decode(tokens[j], UTF8_CHARSET);
                } catch (UnsupportedEncodingException ignore) {}
            }
            
            switch (tokens.length) {
                case 1:
                    if (pair.charAt(0) == '=')
                        map.put("", tokens[0]);
                    else
                        map.put(tokens[0], "");

                    break;
                case 2:
                    map.put(tokens[0], tokens[1]);
                    break;
            }
        }
        return map;
    }
}
