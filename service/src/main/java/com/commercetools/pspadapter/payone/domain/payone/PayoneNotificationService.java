package com.commercetools.pspadapter.payone.domain.payone;


import com.commercetools.pspadapter.payone.domain.payone.model.common.Notification;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import spark.Request;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


/**
 * @author fhaertig
 * @date 17.12.15
 */
public class PayoneNotificationService {

    private static final Logger LOG = LogManager.getLogger(PayoneNotificationService.class);
    private final NotificationDispatcher notificationDispatcher;

    public PayoneNotificationService(final NotificationDispatcher notificationDispatcher) {
        this.notificationDispatcher = notificationDispatcher;

    }

    public Notification receiveNotification(Request payoneRequest) throws IOException {

        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        String json = mapper.writeValueAsString(buildMapFromRequestParams(payoneRequest.body()));

        Notification notification = Notification.fromJsonString(json);
        notificationDispatcher.dispatchNotification(notification);
        return notification;
    }

    Map<String, String> buildMapFromRequestParams(final String serverRequest) {
        Map<String, String> resultMap = new HashMap<String, String>();

        String[] properties = serverRequest.split("\\r?\\n?&");
        for (String property : properties) {
            String[] param = StringUtils.split(property, "=", 2);
            if (param != null && param.length > 1) {
                resultMap.put(param[0], param[1]);
            }
        }
        return resultMap;
    }
}
