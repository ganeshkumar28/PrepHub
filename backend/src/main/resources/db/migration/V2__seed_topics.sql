-- V2__seed_topics.sql
-- Seed data: expanded topics taxonomy
-- Run after V1__init.sql. Gives Gemini a fixed vocabulary to match against during extraction,
-- instead of inventing free-text tags that fragment ("Multithreading" vs "multi-threading" vs "Threads").

insert into topics (name, slug, kind) values
  -- LANGUAGE
  ('Java', 'java', 'LANGUAGE'),
  ('Python', 'python', 'LANGUAGE'),
  ('JavaScript', 'javascript', 'LANGUAGE'),
  ('TypeScript', 'typescript', 'LANGUAGE'),
  ('C++', 'cpp', 'LANGUAGE'),
  ('Go', 'go', 'LANGUAGE'),

  -- FRAMEWORK
  ('Spring Boot', 'spring-boot', 'FRAMEWORK'),
  ('Spring Security', 'spring-security', 'FRAMEWORK'),
  ('Spring Data JPA', 'spring-data-jpa', 'FRAMEWORK'),
  ('Hibernate', 'hibernate', 'FRAMEWORK'),
  ('Spring Batch', 'spring-batch', 'FRAMEWORK'),
  ('React', 'react', 'FRAMEWORK'),
  ('Angular', 'angular', 'FRAMEWORK'),
  ('Node.js', 'nodejs', 'FRAMEWORK'),
  ('FastAPI', 'fastapi', 'FRAMEWORK'),
  ('PyTorch', 'pytorch', 'FRAMEWORK'),

  -- DATABASE
  ('SQL', 'sql', 'DATABASE'),
  ('Database Indexing', 'database-indexing', 'DATABASE'),
  ('Transactions & ACID', 'transactions-acid', 'DATABASE'),
  ('MySQL', 'mysql', 'DATABASE'),
  ('PostgreSQL', 'postgresql', 'DATABASE'),
  ('MongoDB', 'mongodb', 'DATABASE'),
  ('Redis', 'redis', 'DATABASE'),
  ('Cloud Spanner', 'cloud-spanner', 'DATABASE'),
  ('BigQuery', 'bigquery', 'DATABASE'),

  -- SYSTEM_DESIGN
  ('System Design Basics', 'system-design-basics', 'SYSTEM_DESIGN'),
  ('Load Balancing', 'load-balancing', 'SYSTEM_DESIGN'),
  ('Caching', 'caching', 'SYSTEM_DESIGN'),
  ('Microservices', 'microservices', 'SYSTEM_DESIGN'),
  ('Message Queues', 'message-queues', 'SYSTEM_DESIGN'),
  ('API Design (REST)', 'api-design-rest', 'SYSTEM_DESIGN'),
  ('Scalability', 'scalability', 'SYSTEM_DESIGN'),
  ('Event-Driven Architecture', 'event-driven-architecture', 'SYSTEM_DESIGN'),
  ('Authentication & OAuth', 'auth-oauth', 'SYSTEM_DESIGN'),

  -- DEVOPS (Using this for Cloud as per OpenAPI enums)
  ('Docker', 'docker', 'DEVOPS'),
  ('Kubernetes', 'kubernetes', 'DEVOPS'),
  ('CI/CD', 'ci-cd', 'DEVOPS'),
  ('Kafka', 'kafka', 'DEVOPS'),
  ('Google Cloud Platform', 'gcp', 'DEVOPS'),
  ('AWS', 'aws', 'DEVOPS'),
  ('Terraform', 'terraform', 'DEVOPS'),

  -- CONCEPT
  ('Multithreading', 'multithreading', 'CONCEPT'),
  ('Collections Framework', 'collections-framework', 'CONCEPT'),
  ('Exception Handling', 'exception-handling', 'CONCEPT'),
  ('Generics', 'generics', 'CONCEPT'),
  ('JVM Internals', 'jvm-internals', 'CONCEPT'),
  ('Garbage Collection', 'garbage-collection', 'CONCEPT'),
  ('Streams API', 'streams-api', 'CONCEPT'),
  ('Machine Learning', 'machine-learning', 'CONCEPT'),
  ('Agentic AI', 'agentic-ai', 'CONCEPT'),
  ('Full Stack Development', 'full-stack-development', 'CONCEPT'),

  -- DSA
  ('Arrays & Strings', 'arrays-strings', 'DSA'),
  ('Linked Lists', 'linked-lists', 'DSA'),
  ('Trees & Graphs', 'trees-graphs', 'DSA'),
  ('Dynamic Programming', 'dynamic-programming', 'DSA'),
  ('Sorting & Searching', 'sorting-searching', 'DSA'),
  ('Time & Space Complexity', 'time-space-complexity', 'DSA'),

  -- BEHAVIORAL
  ('Teamwork & Collaboration', 'teamwork-collaboration', 'BEHAVIORAL'),
  ('Conflict Resolution', 'conflict-resolution', 'BEHAVIORAL'),
  ('Leadership', 'leadership', 'BEHAVIORAL'),
  ('Project Ownership', 'project-ownership', 'BEHAVIORAL')
on conflict (slug) do nothing;