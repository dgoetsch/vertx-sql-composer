package dev.yn.playground.user.sql

import dev.yn.playground.common.ApplicationContext
import dev.yn.playground.sql.*
import dev.yn.playground.ex.dropTableIfExists
import dev.yn.playground.ex.exec
import dev.yn.playground.Task

object UserSchema {

    val createUserProfileTable = """CREATE TABLE IF NOT EXISTS user_profile (
    id text PRIMARY KEY,
    user_name text UNIQUE,
    allow_public_message boolean
);"""

    val createUserPasswordTable = """CREATE TABLE IF NOT EXISTS user_password (
    user_id text REFERENCES user_profile(id) PRIMARY KEY,
    pswhash text
);
"""
    val userSessionUserIdUniqueConstraintName = "user_session_user_id_unique"

    val createUserSessionTable = """CREATE TABLE IF NOT EXISTS user_session(
    session_key text PRIMARY KEY,
    user_id text REFERENCES user_profile(id) CONSTRAINT ${userSessionUserIdUniqueConstraintName} UNIQUE,
    expiration timestamp
);"""

    val createUserRequestTable = """
CREATE TABLE IF NOT EXISTS user_relationship_request (
    user_id_1 text REFERENCES user_profile(id),
    user_id_2 text REFERENCES user_profile(id),
    initiated timestamp,
    initiator text,
    CONSTRAINT user_relationship_request_pk PRIMARY KEY (user_id_1, user_id_2)
);"""

    fun init() = Task.identity<Unit, ApplicationContext>()
            .exec(SQLMapping.exec("CREATE EXTENSION IF NOT EXISTS pgcrypto"))
            .exec(SQLMapping.exec(createUserProfileTable))
            .exec(SQLMapping.exec(createUserPasswordTable))
            .exec(SQLMapping.exec(createUserSessionTable))
            .exec(SQLMapping.exec(createUserRequestTable))

    fun drop() = Task.identity<Unit, ApplicationContext>()
            .dropTableIfExists("user_relationship_request")
            .dropTableIfExists("user_password")
            .dropTableIfExists("user_session")
            .dropTableIfExists("user_profile")
}