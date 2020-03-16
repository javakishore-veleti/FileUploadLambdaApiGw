CURR_DIR=$(dirname $0)

ROLE_NAME="jk-aws-lambda-apigw-fileupload-lambda-role"
LAMBDA_FUNCTION_DATE="jk-aws-lambda-apigw-fileupload-fn"
S3_BUCKET_NAME="jk-aws-lambda-apigw-fileupload-2020mar16"
AWS_ACCOUNT_NUMBER=account-id

aws s3api create-bucket --acl private --bucket ${S3_BUCKET_NAME} --region us-east-1

aws iam create-role --role-name ${ROLE_NAME} --assume-role-policy-document file://${CURR_DIR}/trustpolicy.json

aws iam attach-role-policy --policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole --role-name ${ROLE_NAME}

aws iam attach-role-policy --policy-arn arn:aws:iam::aws:policy/AWSLambdaExecute --role-name ${ROLE_NAME}

aws lambda create-function --function-name ${LAMBDA_FUNCTION_DATE} --runtime java8 --role arn:aws:iam::${AWS_ACCOUNT_NUMBER}:role/${ROLE_NAME} --handler jk.aws.lambda.handlers.FileUploadHandler::handleRequest --zip-file fileb://${CURR_DIR}/target/fileupload-lambda-apigw-0.0.1-SNAPSHOT.jar --region us-east-1 --timeout 60 --memory-size 320

# aws lambda update-function-code --function-name [fileupload] --zip-file fileb://./target/fileupload-0.0.1-SNAPSHOT.jar --region [us-east-1]

# aws lambda update-function-code --function-name ${LAMBDA_FUNCTION_DATE} --zip-file fileb://${CURR_DIR}/target/fileupload-lambda-apigw-0.0.1-SNAPSHOT.jar --region us-east-1

https://fz4t9qn2u4.execute-api.us-east-1.amazonaws.com/Development

curl -v -X POST "https://[URI-ID].execute-api.us-east-1.amazonaws.com/Development" -F "data=@sample-picture-for-upload.png"

curl -v -X POST "https://[URI-ID].execute-api.us-east-1.amazonaws.com/Development/fileupload"  -F "data=@sample-picture-for-upload.png"

# https://[URI-ID].execute-api.us-east-1.amazonaws.com/Development