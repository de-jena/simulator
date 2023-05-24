FROM eclipse-temurin:17-jre-alpine

ENV USERNAME=osgi \
	HOME_DIR=/osgi \
	OPT_DIR=/opt/osgi \
	GECKO_VERSION=2.0.0

RUN mkdir -p ${OPT_DIR} && \
    mkdir -p ${OPT_DIR}/logs && \
    mkdir -p /tmp

COPY de.jena.publictransport.simulator/generated/distributions/executable/launch.jar ${OPT_DIR}/

RUN mkdir ${HOME_DIR}

RUN addgroup -g 7743 -S ${USERNAME} && \
    adduser -u 7743 -h ${HOME_DIR} -s /bin/false -S ${USERNAME} -G ${USERNAME}

RUN chown -R ${USERNAME} ${OPT_DIR} && \
    chmod -R u+rwx ${OPT_DIR} && \
    chown -R ${USERNAME} /tmp && \
    chmod -R u+rw /tmp

RUN cd ${OPT_DIR}

VOLUME ${HOME_DIR}

WORKDIR ${OPT_DIR}
USER ${USERNAME}

CMD ["java", "-jar", "/opt/osgi/launch.jar"]
