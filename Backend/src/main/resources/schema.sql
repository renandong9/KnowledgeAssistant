CREATE TABLE IF NOT EXISTS documents (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(255) NOT NULL,
  original_file_name VARCHAR(255) NOT NULL,
  file_path VARCHAR(500) NOT NULL,
  file_type VARCHAR(20) NOT NULL,
  file_size BIGINT NOT NULL,
  parse_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
  summary TEXT NULL,
  error_message TEXT NULL,
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  INDEX idx_documents_create_time (create_time),
  INDEX idx_documents_parse_status (parse_status)
);

CREATE TABLE IF NOT EXISTS document_chunks (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  document_id BIGINT NOT NULL,
  chunk_index INT NOT NULL,
  content MEDIUMTEXT NOT NULL,
  page_number INT NULL,
  position_hint VARCHAR(100) NULL,
  embedding MEDIUMTEXT NULL,
  token_count INT NULL,
  create_time DATETIME NOT NULL,
  INDEX idx_document_chunks_document_id (document_id),
  FULLTEXT INDEX ft_document_chunks_content (content),
  CONSTRAINT fk_chunks_document
    FOREIGN KEY (document_id) REFERENCES documents(id)
    ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS chat_sessions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(255) NOT NULL,
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  INDEX idx_chat_sessions_update_time (update_time)
);

CREATE TABLE IF NOT EXISTS chat_messages (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  session_id BIGINT NOT NULL,
  role VARCHAR(20) NOT NULL,
  content MEDIUMTEXT NOT NULL,
  references_json JSON NULL,
  create_time DATETIME NOT NULL,
  INDEX idx_chat_messages_session_id (session_id),
  INDEX idx_chat_messages_create_time (create_time),
  CONSTRAINT fk_messages_session
    FOREIGN KEY (session_id) REFERENCES chat_sessions(id)
    ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS review_reports (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  document_id BIGINT NULL,
  title VARCHAR(255) NOT NULL,
  summary MEDIUMTEXT NOT NULL,
  key_points MEDIUMTEXT NULL,
  questions MEDIUMTEXT NULL,
  improvement_advice MEDIUMTEXT NULL,
  create_time DATETIME NOT NULL,
  INDEX idx_review_reports_document_id (document_id)
);

CREATE TABLE IF NOT EXISTS paper_recommendations (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  source VARCHAR(50) NOT NULL,
  query_text VARCHAR(500) NOT NULL,
  title VARCHAR(500) NOT NULL,
  authors VARCHAR(1000) NULL,
  published_year INT NULL,
  abstract_text MEDIUMTEXT NULL,
  url VARCHAR(1000) NULL,
  reason MEDIUMTEXT NULL,
  create_time DATETIME NOT NULL,
  INDEX idx_paper_query (query_text)
);
