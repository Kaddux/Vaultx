#!/bin/bash
set -e
awslocal s3 mb s3://vaultx-media

cat <<'EOF' > /tmp/policy.json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": "*",
      "Action": ["s3:GetObject"],
      "Resource": "arn:aws:s3:::vaultx-media/*"
    }
  ]
}
EOF
awslocal s3api put-bucket-policy --bucket vaultx-media --policy file:///tmp/policy.json

cat <<'EOF' > /tmp/cors.json
{
  "CORSRules": [
    {
      "AllowedOrigins": ["*"],
      "AllowedMethods": ["GET", "PUT", "HEAD"],
      "AllowedHeaders": ["*"],
      "ExposeHeaders": ["ETag", "x-amz-meta-*"],
      "MaxAgeSeconds": 3600
    }
  ]
}
EOF
awslocal s3api put-bucket-cors --bucket vaultx-media --cors-configuration file:///tmp/cors.json
