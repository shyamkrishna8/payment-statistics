CREATE SEQUENCE transactionamount_id_seq
  INCREMENT 1
  MINVALUE 1
  MAXVALUE 9223372036854775807
  START 2000
  CACHE 1;

CREATE TABLE transactionamount
(
  "id" integer NOT NULL DEFAULT nextval('transactionamount_id_seq'::regclass),
  "created" bigint,
  "amount" double precision,
  "time_stamp" bigint,
  CONSTRAINT transactionamount_pkey PRIMARY KEY ("id")
);
