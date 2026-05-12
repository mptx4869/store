class BookMatcher:
    
    def __init__(self, index_path: str = "models/faiss_index.bin", mapping_path: str = "models/index_mapping.json"):
        self.index_path = index_path
        self.mapping_path = mapping_path
        # self.index = faiss.read_index(index_path)
        # self.mapping = json.load(open(mapping_path))
        
    def match_books(self, goodreads_book_names: list[str], top_k: int = 1):
        
        # TODO: Cài đặt logic query FAISS
        pass
