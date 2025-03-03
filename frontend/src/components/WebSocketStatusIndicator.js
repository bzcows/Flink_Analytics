import React from 'react';

const WebSocketStatusIndicator = ({ status }) => {
  const getStatusDisplay = () => {
    switch (status) {
      case 'connected':
        return (
          <>
            <svg
              xmlns="http://www.w3.org/2000/svg"
              width="12"
              height="12"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
              className="status-icon connected"
            >
              <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
              <polyline points="22 4 12 14.01 9 11.01"></polyline>
            </svg>
            <span className="status-text">CONNECTED</span>
          </>
        );
      case 'disconnected':
      case 'error':
        return (
          <>
            <svg
              xmlns="http://www.w3.org/2000/svg"
              width="12"
              height="12"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
              className="status-icon disconnected"
            >
              <circle cx="12" cy="12" r="10"></circle>
              <line x1="15" y1="9" x2="9" y2="15"></line>
              <line x1="9" y1="9" x2="15" y2="15"></line>
            </svg>
            <span className="status-text">DISCONNECTED</span>
          </>
        );
      default:
        return null;
    }
  };

  return (
    <div className={`websocket-status-indicator ${status}`}>
      {getStatusDisplay()}
    </div>
  );
};

export default WebSocketStatusIndicator;