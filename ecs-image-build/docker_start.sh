#!/bin/bash
#
# Start script for company-appointment-consumer
exec java -jar -Dserver.port="${PORT}" "company-appointment-consumer.jar"