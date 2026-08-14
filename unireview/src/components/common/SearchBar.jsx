import { useState } from 'react';
import { FiSearch } from 'react-icons/fi';
import './SearchBar.css';

export default function SearchBar({ placeholder = 'Tìm kiếm...', onSearch, value = '' }) {
  const [query, setQuery] = useState(value);

  const handleSubmit = (e) => {
    e.preventDefault();
    onSearch?.(query);
  };

  return (
    <form className="search-bar" onSubmit={handleSubmit}>
      <FiSearch className="search-icon" />
      <input
        type="text"
        className="search-input"
        placeholder={placeholder}
        value={query}
        onChange={(e) => setQuery(e.target.value)}
      />
      <button type="submit" className="btn btn-primary search-btn">
        Tìm kiếm
      </button>
    </form>
  );
}
