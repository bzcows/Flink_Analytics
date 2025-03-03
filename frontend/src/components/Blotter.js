import React, { useState, useRef, useEffect } from 'react';

function Blotter({ transactions }) {
  const [rate, setRate] = useState(1);
  const [message, setMessage] = useState('');
  const [isRunning, setIsRunning] = useState(false);
  const toggleButtonRef = useRef(null);
  const [showConfigModal, setShowConfigModal] = useState(false);
  const [columnConfig, setColumnConfig] = useState({
    txNumber: true,
    merchantId: true,
    ccType: true,
    ccNumber: true,
    merchantType: true,
    txLocation: true,
    amount: true
  });

  // Load saved column configuration from localStorage on component mount
  useEffect(() => {
    const savedConfig = localStorage.getItem('blotterColumnConfig');
    if (savedConfig) {
      setColumnConfig(JSON.parse(savedConfig));
    }
  }, []);

  const calculateStep = (value) => {
    return parseInt(value) >= 10 ? 10 : 1;
  }

  const handleRateChange = async (event) => {
    const newRate = parseInt(event.target.value);
    setRate(newRate);
     try {
      const response = await fetch('http://localhost:8080/api/fake-transactions/setRate', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ rate: newRate }),
      });
      const data = await response.text();
      setMessage("tps");
    } catch (error) {
      setMessage('Error setting rate: ' + error);
    }
  };

  const toggleStream = async () => {
    try {
      const response = await fetch('http://localhost:8080/api/fake-transactions/toggle', {
        method: 'GET'
      });
      const result = await response.text();
      const isStarted = result === 'Started';
      setIsRunning(isStarted);
    } catch (error) {
      console.error('Error toggling transactions:', error);
    }
  };

  const toggleConfigModal = () => {
    setShowConfigModal(!showConfigModal);
  };

  const handleColumnConfigChange = (column) => {
    const updatedConfig = {
      ...columnConfig,
      [column]: !columnConfig[column]
    };
    setColumnConfig(updatedConfig);
  };

  const saveColumnConfig = () => {
    localStorage.setItem('blotterColumnConfig', JSON.stringify(columnConfig));
    setShowConfigModal(false);
  };

  const cardTypeIcons = {
    MASTERCARD: <img src="/mastercard_icon.ico" alt="MasterCard" width="24" height="24" />,
    VISA: <img src="/visa_icon.ico" alt="VISA" width="24" height="24" />,
  };

  // Define column labels for the configuration modal
  const columnLabels = {
    txNumber: 'Transaction ID',
    merchantId: 'Merchant ID',
    ccType: 'Card Type',
    ccNumber: 'Card Number',
    merchantType: 'Merchant Type',
    txLocation: 'Location',
    amount: 'Amount'
  };

  return (
    <div>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'flex-start', flexWrap: 'wrap' }}>
        <span className="title">Live Transactions</span>
        <div className="slider-container" style={{
          boxShadow: '0 2px 4px 0 rgba(0,0,0,0.2)',
          padding: '10px',
          borderRadius: '5px',
          display: 'flex',
          alignItems: 'center',
          gap: '10px'
        }}>
          <button
            ref={toggleButtonRef}
            className={`control-btn ${isRunning ? 'stop' : 'start'}`}
            onClick={toggleStream}
            style={{
              padding: '5px 10px',
              borderRadius: '5px',
              cursor: 'pointer'
            }}
          >
            {isRunning ? 'Stop' : 'Start'}
          </button>
          <span>Tx Rate:</span>
          <input
            type="range"
            id="rate"
            min="1"
            max="100"
            value={rate}
            step={() => calculateStep(document.getElementById('rate')?.value || rate)}
            onChange={handleRateChange}
            style={{
              '--slider-track-color': 'grey',
              '--slider-thumb-color': 'blue',
              appearance: 'none',
              width: '200px',
              height: '10px',
              background: 'var(--slider-track-color)',
              borderRadius: '5px',
              outline: 'none',
            }}
            className="slider"
          />
          <style>
            {`
              .slider::-webkit-slider-thumb {
                -webkit-appearance: none;
                appearance: none;
                width: 20px;
                height: 20px;
                background: var(--slider-thumb-color);
                border-radius: 50%;
                cursor: pointer;
              }

              .slider::-moz-range-thumb {
                width: 20px;
                height: 20px;
                background: var(--slider-thumb-color);
                border-radius: 50%;
                cursor: pointer;
              }

              @media (max-width: 768px) {
                .slider-container {
                  width: 100%;
                  margin-top: 10px;
                }
              }
            `}
          </style>
          <span style={{ marginLeft: '10px' }}>{rate}</span>
          <span style={{ marginLeft: '10px' }}>{message}</span>
          
          {/* Configuration Icon */}
          <button 
            onClick={toggleConfigModal}
            style={{
              marginLeft: '10px',
              padding: '5px 10px',
              borderRadius: '5px',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              border: 'none'
            }}
            title="Configure Columns"
          >
            <span role="img" aria-label="Configure Columns" style={{ fontSize: '16px' }}>⚙️</span>
          </button>
        </div>
      </div>
      
      {/* Column Configuration Modal */}
      {showConfigModal && (
        <div style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          backgroundColor: 'rgba(0, 0, 0, 0.5)',
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          zIndex: 1000
        }}>
          <div style={{
            backgroundColor: 'white',
            padding: '20px',
            borderRadius: '5px',
            width: '400px',
            maxWidth: '90%',
            boxShadow: '0 4px 8px rgba(0, 0, 0, 0.2)'
          }}>
            <h3 style={{ marginTop: 0 }}>Configure Columns</h3>
            <div style={{ marginBottom: '15px' }}>
              {Object.keys(columnLabels).map(column => (
                <div key={column} style={{ marginBottom: '10px' }}>
                  <label style={{ display: 'flex', alignItems: 'center' }}>
                    <input
                      type="checkbox"
                      checked={columnConfig[column]}
                      onChange={() => handleColumnConfigChange(column)}
                      style={{ marginRight: '10px' }}
                    />
                    {columnLabels[column]}
                  </label>
                </div>
              ))}
            </div>
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px' }}>
              <button 
                onClick={() => setShowConfigModal(false)}
                style={{
                  padding: '5px 10px',
                  borderRadius: '5px',
                  cursor: 'pointer',
                  backgroundColor: '#f0f0f0'
                }}
              >
                Cancel
              </button>
              <button 
                onClick={saveColumnConfig}
                style={{
                  padding: '5px 10px',
                  borderRadius: '5px',
                  cursor: 'pointer',
                  backgroundColor: '#4CAF50',
                  color: 'white',
                  border: 'none'
                }}
              >
                Save
              </button>
            </div>
          </div>
        </div>
      )}
      
      <div className="blotter">
        <table>
          <thead>
            <tr>
              {columnConfig.txNumber && <th>Transaction ID</th>}
              {columnConfig.merchantId && <th>Merchant ID</th>}
              {columnConfig.ccType && <th>Card Type</th>}
              {columnConfig.ccNumber && <th>Card Number</th>}
              {columnConfig.merchantType && <th>Merchant Type</th>}
              {columnConfig.txLocation && <th>Location</th>}
              {columnConfig.amount && <th>Amount</th>}
            </tr>
          </thead>
          <tbody>
            {transactions.map(tx => (
              <tr key={tx.txNumber}>
                {columnConfig.txNumber && <td>{tx.txNumber}</td>}
                {columnConfig.merchantId && <td>{tx.merchantId}</td>}
                {columnConfig.ccType && <td>{cardTypeIcons[tx.ccType] || tx.ccType}</td>}
                {columnConfig.ccNumber && <td>{tx.ccNumber ? `**** **** **** ${tx.ccNumber.slice(-4)}` : 'N/A'}</td>}
                {columnConfig.merchantType && <td>{tx.merchantType || 'N/A'}</td>}
                {columnConfig.txLocation && (
                  <td>
                    {tx.txLocation ? 
                      `${tx.txLocation.latitude.toFixed(2)}, ${tx.txLocation.longitude.toFixed(2)}` : 
                      (tx.merchantType === 'ONLINE' ? 'ONLINE' : 'N/A')}
                  </td>
                )}
                {columnConfig.amount && <td>${tx.amount.toFixed(2)}</td>}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

export default Blotter;