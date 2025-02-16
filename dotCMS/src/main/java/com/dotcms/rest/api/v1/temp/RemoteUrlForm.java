package com.dotcms.rest.api.v1.temp;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RemoteUrlForm {

  public final String remoteUrl;
  public final String fileName;
  public final String accessKey;
  public final Integer urlTimeoutSeconds;

  protected RemoteUrlForm(@JsonProperty("remoteUrl") final String remoteUrl, @JsonProperty("fileName") final String fileName,
      @JsonProperty("accessKey") final String accessKey) {
    this(remoteUrl, fileName, accessKey, null);
  }

  @JsonCreator
  protected RemoteUrlForm(@JsonProperty("remoteUrl") final String remoteUrl, @JsonProperty("fileName") final String fileName,
      @JsonProperty("accessKey") final String accessKey, @JsonProperty("urlTimeoutSeconds") final Integer urlTimeout) {
    super();
    this.remoteUrl = remoteUrl;
    this.fileName = fileName;
    this.accessKey = accessKey;
    this.urlTimeoutSeconds = urlTimeout != null && urlTimeout < 600 ? urlTimeout : 30;
  }

}
