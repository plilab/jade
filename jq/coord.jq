def coord(): [.groupId, .artifactId, .fileExtension, .classifier, .version] | map(select(. != "")) | join(":")
