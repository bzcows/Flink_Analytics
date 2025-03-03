import React, { useState, useRef } from 'react';
import QueryVisualization from './QueryVisualization';

const AggregationPanel = ({ query, result, onDelete }) => {
  const [isMinimized, setIsMinimized] = useState(false);
  const [position, setPosition] = useState({ x: 0, y: 0 });
  const [isDragging, setIsDragging] = useState(false);
  const dragStartPos = useRef({ x: 0, y: 0 });

  const toggleMinimize = () => {
    setIsMinimized(!isMinimized);
  };

  const handleMouseDown = (e) => {
    if (e.target.closest('.minimize-btn, .delete-btn')) return;
    setIsDragging(true);
    dragStartPos.current = {
      x: e.clientX - position.x,
      y: e.clientY - position.y
    };
  };

  const handleMouseMove = (e) => {
    if (!isDragging) return;
    
    const newX = e.clientX - dragStartPos.current.x;
    const newY = e.clientY - dragStartPos.current.y;
    
    setPosition({ x: newX, y: newY });
  };

  const handleMouseUp = () => {
    setIsDragging(false);
  };

  return (
    <div 
      className={`aggregation-panel ${isMinimized ? 'minimized' : ''}`}
      style={{
        position: 'absolute',
        left: `${position.x}px`,
        top: `${position.y}px`,
        cursor: isDragging ? 'grabbing' : 'grab',
        zIndex: isDragging ? 1000 : 1
      }}
      onMouseDown={handleMouseDown}
      onMouseMove={handleMouseMove}
      onMouseUp={handleMouseUp}
      onMouseLeave={handleMouseUp}
      onClick={isMinimized ? toggleMinimize : undefined}
      title={isMinimized ? `SQL Query: ${query}` : ''}
    >
      <div className="aggregation-header">
        <div className="header-controls">
          <button className="minimize-btn" onClick={toggleMinimize}>
            {isMinimized ? '▲' : '▼'}
          </button>
          <div className="header-title">
           <h3>RULE</h3> 
          </div>
          <button className="delete-btn" onClick={onDelete}></button>
        </div>
      </div>
      <div className="panel-content">
        <h4>Query:</h4>
        <div className="query-section">
          <pre>{query}</pre>
        </div>
        <div className="result-section">
          <h4>Result:</h4>
          {result ? (
            <QueryVisualization data={result} />
          ) : (
            <p>No results yet.</p>
          )}
        </div>
      </div>
    </div>
  );
};

export default AggregationPanel;