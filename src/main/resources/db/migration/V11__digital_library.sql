-- =====================================================
-- Digital Library / E-Books
-- =====================================================

CREATE TABLE book_categories (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    school_id UUID REFERENCES schools(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(school_id, name)
);

CREATE TABLE library_books (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    school_id UUID REFERENCES schools(id) ON DELETE CASCADE,
    category_id UUID REFERENCES book_categories(id),
    title VARCHAR(255) NOT NULL,
    author VARCHAR(255),
    isbn VARCHAR(50),
    publisher VARCHAR(255),
    edition VARCHAR(50),
    description TEXT,
    cover_image_url TEXT,
    file_url TEXT,
    file_type VARCHAR(20) DEFAULT 'PDF' CHECK (file_type IN ('PDF', 'EPUB', 'AUDIO', 'VIDEO', 'LINK')),
    total_copies INT DEFAULT 1,
    available_copies INT DEFAULT 1,
    is_digital BOOLEAN DEFAULT TRUE,
    is_active BOOLEAN DEFAULT TRUE,
    tags TEXT[],
    metadata JSONB DEFAULT '{}',
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE book_borrowals (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    book_id UUID REFERENCES library_books(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    borrow_date DATE DEFAULT CURRENT_DATE,
    due_date DATE,
    return_date DATE,
    status VARCHAR(20) DEFAULT 'BORROWED' CHECK (status IN ('BORROWED', 'RETURNED', 'OVERDUE', 'LOST')),
    fine_amount DECIMAL(10,2) DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_library_books_school ON library_books(school_id);
CREATE INDEX idx_library_books_category ON library_books(category_id);
CREATE INDEX idx_library_books_title ON library_books(school_id, title);
CREATE INDEX idx_book_borrowals_user ON book_borrowals(user_id);
CREATE INDEX idx_book_borrowals_status ON book_borrowals(status);

CREATE TRIGGER update_library_books_updated_at BEFORE UPDATE ON library_books
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_book_borrowals_updated_at BEFORE UPDATE ON book_borrowals
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
