# Zeebe Protocols Reference

Zeebe uses binary protocols for client to broker and broker to broker communication.

Zeebe "speaks" 4 protocols:

* Client Protocol: client to broker communication
* Raft Protocol: broker to broker communication for data replication and leadership election
* Gossip Protocol: broker to broker communication for cluster membership, node failure detection and dissemination of cluster topology and state
* Management Protocol: broker to broker communication for additional management & administrative operations like raft group invites, creation and removal of topics and more

For these protocols, the following default ports are used:

* 51015: Client Protocol
* 51016: Gossip Protocol + Management Protocol
* 51017: Raft Protocol

## Basics

Zeebe uses a layered approach to it's protocols:

```
+---------------------------------------------------------------+
|                      Data Frame Header                        |
+---------------------------------------------------------------+
|                   Transport Protocol Header                   |
+---------------------------------------------------------------+
|                           Protocol                            |
+---------------------------------------------------------------+
```

## Data Frames

```
+---------------------------------------------------------------+
|               Data Frame Header (incl. message length)        |
+---------------------------------------------------------------+
|                                                              ...
|                            Message                            |
...                                                             |
+---------------------------------------------------------------+
```

The bottom protocol layer is a _framing protocol_ which defines data frames of certain lengths. Intuitively, this protocol layer defines _messages_ of a certain length and arbitrary payload. The length of the header is 12 bytes. The most important entry in the header is the length which defines the length of the framed message in bytes. Details about the data frame header and the contained fields can be found [here](https://github.com/zeebe-io/zb-dispatcher/wiki/Log-Buffer#fragment-layout).

> Note: when consuming multiple messages as a stream, be aware that messages are 8-bytes aligned. This means that, after consuming a message, the next message will start at an offset which can be calculated by `next_msg_offset = curr_msg_offset + header_length + curr_msg_length + x` where `x` is a number `>= 0` such that `next_msg_offset % 8 = 0` holds).

## Transport Protocol Header

The transport protocol header defines an additional header which allows to differentiate [zb-transport](https://github.com/zeebe-io/zb-transport)'s request/response vs. single message protocols.

If the message is part of a request/response interaction, it contains a header field with the `request-id` which must be included in the response message so that the client can correlate request and response messages.

The details about the protocol headers can be found here(TODO).

## Client Protocol

The Zeebe Client Protocol is the OSI Layer 7 protocol and is defined in SBE ([Simple Binary Encoding](https://github.com/real-logic/simple-binary-encoding)).

The corresponding descriptor can be found [here](https://github.com/zeebe-io/zb-protocol/blob/master/src/main/resources/protocol.xml).

The most important message defined in this protocol is the `ExecuteCommandRequest` message. This message allows the client to send a `command` to the broker which is executed with event sourcing semantics.

### Client Protocol Commands Reference

Commands are formulated in [Message Pack](http://msgpack.org/) encoded Json.

### Create Task

**Required Fields**

* `state` (String): must have value `CREATE`
* `type` (String): user-defined, the type of the task, eg. `update-customer-addr`

**Optional Fields**

* `retries`: default value is `-1`
* `customHeaders`: user-defined, custom headers for the task, key value map with String keys, default it empty map.
* `payload`: binary, must contain message-pack encoded json

**Example**

```json
{
    "state": "CREATE",
    "type": "update-customer-addr",
    "retries": 3,
    "customHeaders": { "db-name": "customers" },
    "payload": [BIN]    
}
```
