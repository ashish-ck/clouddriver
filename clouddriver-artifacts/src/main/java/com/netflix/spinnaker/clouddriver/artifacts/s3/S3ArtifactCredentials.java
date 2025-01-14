/*
 * Copyright 2018 Datadog, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.clouddriver.artifacts.s3;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.PredefinedClientConfigurations;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3Object;
import com.google.common.collect.ImmutableList;
import com.netflix.spinnaker.clouddriver.artifacts.config.ArtifactCredentials;
import com.netflix.spinnaker.kork.annotations.NonnullByDefault;
import com.netflix.spinnaker.kork.artifacts.model.Artifact;
import java.io.InputStream;
import java.util.Optional;
import javax.annotation.Nullable;
import lombok.Getter;

@NonnullByDefault
public class S3ArtifactCredentials implements ArtifactCredentials {
  public static final String CREDENTIALS_TYPE = "artifacts-s3";
  @Getter private final String name;
  @Getter private final ImmutableList<String> types = ImmutableList.of("s3/object");

  private final String apiEndpoint;
  private final String apiRegion;
  private final String region;
  private final String awsAccessKeyId;
  private final String awsSecretAccessKey;
  private final String signerOverride;
  private final Optional<S3ArtifactValidator> s3ArtifactValidator;

  private AmazonS3 amazonS3;

  S3ArtifactCredentials(
      S3ArtifactAccount account, Optional<S3ArtifactValidator> s3ArtifactValidator) {
    this(account, s3ArtifactValidator, null);
  }

  S3ArtifactCredentials(S3ArtifactAccount account, @Nullable AmazonS3 amazonS3) {
    this(account, Optional.empty(), amazonS3);
  }

  S3ArtifactCredentials(
      S3ArtifactAccount account,
      Optional<S3ArtifactValidator> s3ArtifactValidator,
      @Nullable AmazonS3 amazonS3) {
    name = account.getName();
    apiEndpoint = account.getApiEndpoint();
    apiRegion = account.getApiRegion();
    region = account.getRegion();
    awsAccessKeyId = account.getAwsAccessKeyId();
    awsSecretAccessKey = account.getAwsSecretAccessKey();
    signerOverride = account.getSignerOverride();
    this.s3ArtifactValidator = s3ArtifactValidator;
    this.amazonS3 = amazonS3;
  }

  private AmazonS3 getS3Client() {
    if (amazonS3 != null) {
      return amazonS3;
    }

    AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard();
    if (!signerOverride.isEmpty()) {
      ClientConfiguration configuration = PredefinedClientConfigurations.defaultConfig();
      configuration.setSignerOverride(signerOverride);
      builder.setClientConfiguration(configuration);
    }
    if (!apiEndpoint.isEmpty()) {
      AwsClientBuilder.EndpointConfiguration endpoint =
          new AwsClientBuilder.EndpointConfiguration(apiEndpoint, apiRegion);
      builder.setEndpointConfiguration(endpoint);
      builder.setPathStyleAccessEnabled(true);
    } else if (!region.isEmpty()) {
      builder.setRegion(region);
    }

    if (!awsAccessKeyId.isEmpty() && !awsSecretAccessKey.isEmpty()) {
      BasicAWSCredentials awsStaticCreds =
          new BasicAWSCredentials(awsAccessKeyId, awsSecretAccessKey);
      builder.withCredentials(new AWSStaticCredentialsProvider(awsStaticCreds));
    }

    amazonS3 = builder.build();
    return amazonS3;
  }

  @Override
  public InputStream download(Artifact artifact) throws IllegalArgumentException {
    String reference = artifact.getReference();
    if (reference.startsWith("s3://")) {
      reference = reference.substring("s3://".length());
    }

    int slash = reference.indexOf("/");
    if (slash <= 0) {
      throw new IllegalArgumentException(
          "S3 references must be of the format s3://<bucket>/<file-path>, got: " + artifact);
    }
    String bucketName = reference.substring(0, slash);
    String path = reference.substring(slash + 1);
    S3Object s3obj = getS3Client().getObject(bucketName, path);
    if (s3ArtifactValidator.isEmpty()) {
      return s3obj.getObjectContent();
    }
    return s3ArtifactValidator.get().validate(getS3Client(), s3obj);
  }

  @Override
  public String getType() {
    return CREDENTIALS_TYPE;
  }
}
