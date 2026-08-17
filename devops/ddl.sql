CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS hstore;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

SELECT extname, extversion
FROM pg_extension
WHERE extname = 'vector';

CREATE TABLE IF NOT EXISTS concept_planning.vector_store (
	id uuid DEFAULT uuid_generate_v4() PRIMARY KEY,
	content text,
	metadata json,
	embedding vector(1536)
)

CREATE INDEX IF NOT EXISTS spring_ai_vector_index_test
ON concept_planning.vector_store
USING HNSW (embedding public.vector_cosine_ops);

SELECT
    amname
FROM pg_am
WHERE amname IN ('hnsw', 'ivfflat');


SELECT
    opcname,
    amname
FROM pg_opclass oc
JOIN pg_am am
    ON oc.opcmethod = am.oid
WHERE opcname = 'vector_cosine_ops';


ALTER EXTENSION vector SET SCHEMA concept_planning;

SELECT n.nspname AS schema_name,
       opc.opcname,
       am.amname
FROM pg_opclass opc
JOIN pg_namespace n ON n.oid = opc.opcnamespace
JOIN pg_am am ON am.oid = opc.opcmethod
WHERE opc.opcname = 'vector_cosine_ops';


SELECT id, content, metadata
FROM vector_store;