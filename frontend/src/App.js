import React, { useEffect, useState, useRef } from 'react';
import Blotter from './components/Blotter';
import AddRuleModal from './components/AddRuleModal';
import AggregationPanel from './components/AggregationPanel';
import WebSocketStatusIndicator from './components/WebSocketStatusIndicator';
import { ThemeProvider, useTheme } from './components/ThemeContext';
import './App.css';

function AppContent() {
  const { isDark, toggleTheme } = useTheme();
  const [transactions, setTransactions] = useState([]);
  const [queryPanels, setQueryPanels] = useState([]);
  const [ws, setWs] = useState(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [deletingQueries, setDeletingQueries] = useState(new Set());
  const [connectionStatus, setConnectionStatus] = useState('disconnected');
  const [isBlotterCollapsed, setIsBlotterCollapsed] = useState(false);

  const toggleButtonRef = useRef(null);

  const [leftWidth, setLeftWidth] = useState(40);
  const [centerWidth, setCenterWidth] = useState(60);
  const containerRef = useRef(null);

  useEffect(() => {
    document.body.setAttribute('data-theme', isDark ? 'dark' : 'light');
  }, [isDark]);

  useEffect(() => {
    const websocket = new WebSocket('ws://localhost:8080/ws');
    setWs(websocket);

    websocket.onopen = () => {
      console.log('WebSocket connection established');
      setConnectionStatus('connected');
    };

    websocket.onmessage = (event) => {
      const data = JSON.parse(event.data);
      if (data.type === 'transactions') {
        setTransactions((prevTransactions) => {
          const allTransactions = [...data.transactions, ...prevTransactions];
          return allTransactions
            .sort((a, b) => b.txTimestamp - a.txTimestamp)
            .slice(0, 10);
        });
      } else if (data.type === 'query_result') {
        try {
          const parsedData = JSON.parse(data.data);
          const dataArray = parsedData.data;
          const dataFields = parsedData.fieldNames;
          const ruleId = data.queryId;

          if (deletingQueries.has(ruleId)) {
            return;
          }

          if (parsedData.type !== 'retract' && ruleId) {
            setQueryPanels((prevPanels) =>
              prevPanels.map((panel) => {
                if (panel.id === ruleId) {
                  const currentResults = panel.result || [];
                  const merchantId = dataArray[0];
                  const newAmount = parseFloat(dataArray[1]);

                  let direction = 'none';
                  const existingEntry = currentResults.find(
                    (result) => result.data[0] === merchantId
                  );

                  if (existingEntry) {
                    const prevAmount = parseFloat(existingEntry.data[1]);
                    direction = newAmount > prevAmount ? 'up' : 'down';
                  }

                  const filteredResults = currentResults.filter(
                    (result) => result.data[0] !== merchantId
                  );

                  return {
                    ...panel,
                    result: [
                      ...filteredResults,
                      {
                        value: parsedData.type,
                        timestamp: new Date().toLocaleString(),
                        data: dataArray,
                        fieldNames: dataFields,
                        direction: direction,
                      },
                    ],
                  };
                }
                return panel;
              })
            );
          }
        } catch (error) {
          console.error('Error parsing query result data:', error);
        }
      } else if (data.type === 'query_deleting') {
        setDeletingQueries((prev) => new Set([...prev, data.queryId]));
      } else if (data.type === 'query_deleted') {
        setDeletingQueries((prev) => {
          const next = new Set(prev);
          next.delete(data.queryId);
          return next;
        });
      }
    };

    websocket.onerror = (error) => {
      console.error('WebSocket error:', error);
      setConnectionStatus('error');
    };

    websocket.onclose = () => {
      console.log('WebSocket connection closed');
      setConnectionStatus('disconnected');
    };

    return () => {
      if (websocket) {
        websocket.close();
      }
    };
  }, []);

  const handleSubmitQuery = (query) => {
    if (ws && ws.readyState === WebSocket.OPEN) {
      const queryId = crypto.randomUUID();
      const newRule = {
        id: queryId,
        sql: query
      };

      setQueryPanels(prevPanels => [
        ...prevPanels,
        { id: queryId, query: newRule, result: null }
      ]);
    
      ws.send(JSON.stringify({
        type: 'query',
        query,
        queryId: queryId
      }));
    } else {
      console.error('WebSocket is not open');
    }
  };

  const handleDeleteRule = (id) => {
    setDeletingQueries(prev => new Set([...prev, id]));

    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify({ type: 'delete_query', queryId: id }));
    } else {
      console.error('WebSocket is not open');
      setDeletingQueries(prev => {
        const next = new Set(prev);
        next.delete(id);
        return next;
      });
    }

    setQueryPanels(prevPanels => prevPanels.filter(panel => panel.id !== id));
  };

  const handleLeftMouseDown = (e) => {
    e.preventDefault();
    const startX = e.clientX;
    const initialLeft = leftWidth;
    const initialCenter = centerWidth;
    const containerWidth = containerRef.current.offsetWidth;

    const handleMouseMove = (e) => {
      const delta = ((e.clientX - startX) / containerWidth) * 100;
      const newLeft = Math.min(Math.max(initialLeft + delta, 10), 90);
      const newCenter = Math.min(Math.max(initialCenter - delta, 10), 90);
      setLeftWidth(newLeft);
      setCenterWidth(newCenter);
    };

    const handleMouseUp = () => {
      window.removeEventListener('mousemove', handleMouseMove);
      window.removeEventListener('mouseup', handleMouseUp);
    };

    window.addEventListener('mousemove', handleMouseMove);
    window.addEventListener('mouseup', handleMouseUp);
  };

  const toggleBlotterCollapse = () => {
    setIsBlotterCollapsed(!isBlotterCollapsed);
    // Adjust widths when collapsing/expanding
    if (!isBlotterCollapsed) {
      // Save the current width before collapsing
      setLeftWidth(5); // Minimal width when collapsed
      setCenterWidth(95); // Give more space to the center panel
    } else {
      // Restore to default or previous width
      setLeftWidth(40);
      setCenterWidth(60);
    }
  };

  return (
    <div className="app">
      <header className="header">
        <div className="title">Flink Dynamic CEP Demo - AI VIBE-CODING Version</div>
        <div className="controls">
          <button
            className="add-rule-btn"
            onClick={() => setIsModalOpen(true)}
          >
            Add Rule
          </button>
          <button className="theme-toggle" onClick={toggleTheme}>
            {isDark ? '☀️' : '🌙'}
          </button>
        </div>
        <WebSocketStatusIndicator status={connectionStatus} />
      </header>
      <AddRuleModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onSubmit={handleSubmitQuery}
      />
      <div className="content">
        <div className="panels" ref={containerRef}>
          <div className={`panel transactions-panel ${isBlotterCollapsed ? 'collapsed' : ''}`} style={{ width: `${leftWidth}%` }}>
            <button className="collapse-button" onClick={toggleBlotterCollapse}>
            </button>
            <div className="blotter-content">
              <Blotter transactions={transactions} />
            </div>
          </div>
          <div className="resize-handle" onMouseDown={handleLeftMouseDown}></div>
          <div className="panel queries-panel" style={{ width: `${centerWidth}%`, display: 'flex', flexWrap: 'nowrap', overflowX: 'auto' }}>
            {queryPanels.map(panel => (
              <AggregationPanel
                key={panel.id}
                query={panel.query.sql}
                result={panel.result}
                onDelete={() => handleDeleteRule(panel.id)}
              />
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}

function App() {
  return (
    <ThemeProvider>
      <AppContent />
    </ThemeProvider>
  );
}

export default App;
