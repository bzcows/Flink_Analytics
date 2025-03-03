import React, { forwardRef, useState, useEffect } from 'react';

const WebSocket = window.WebSocket;

const QueryForm = ({ rules, onDelete, setRef, isLoading, setIsLoading }, ref) => {
  const [ws, setWs] = useState(null);

  useEffect(() => {
    const socket = new WebSocket('ws://localhost:8080/ws');
    setWs(socket);

    socket.onopen = () => {
      console.log('WebSocket connection established');
    };

    socket.onmessage = (message) => {
      const data = JSON.parse(message.data);
      if (data.type === 'query_started') {
        console.log('Query started:', data.queryId);
      } else if (data.type === 'query_complete') {
        console.log('Query completed:', data.queryId);
        setIsLoading(false);
      } else if (data.type === 'query_error') {
        console.error('Query error:', data.message);
        setIsLoading(false);
      }
    };

    socket.onclose = () => {
      console.log('WebSocket connection closed');
    };

    return () => {
      socket.close();
    };
  }, [setIsLoading]);

  const handleSubmit = (rule) => {
    if (ws && ws.readyState === WebSocket.OPEN) {
      const queryId = `query-${Date.now()}`;
      setIsLoading(true);
      ws.send(JSON.stringify({
        type: 'query',
        query: rule.sql,
        queryId: queryId
      }));
    } else {
      console.error('WebSocket is not connected');
    }
  };

  return (
    <div className="query-form" style={{ position: 'relative' }}>
      {rules.map((rule) => (
        <div
          key={rule.id}
          className="rule-container"
          data-id={rule.id}
          ref={el => setRef && setRef(rule.id, el)}
        >
          <div className="rule-header">
            <span>Rule: {rule.id}</span>
            
            <button
              className="delete-btn"
              onClick={() => onDelete(rule.id)}
            >
              Delete
              <div className="query-spinner"></div>
            </button>
            

          </div>
          <pre className="sql-content">
            {rule.sql}
          </pre>
          
        </div>
      ))}
      {rules.length === 0 && (
        <p>No rules defined.</p>
      )}
    </div>
  );
};

export default forwardRef(QueryForm);