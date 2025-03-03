import React, { useState } from 'react';

function AddRuleModal({ isOpen, onClose, onSubmit }) {
  const [sql, setSql] = useState('');

  const handleSubmit = () => {
    console.log('handleSubmit called, sql:', sql);
    onSubmit(sql);
    setSql('');
    onClose();
  };

  const templates = {
    'COUNT': 'SELECT \'TotalTx\', COUNT(*)\nFROM Transactions ',
    'GROUP BY': 'SELECT merchantId AS merchantId, MIN(amount) AS minAmount, MAX(amount) AS maxAmount \nFROM Transactions \nGROUP BY merchantId',
    'MATCH': 'SELECT t.merchantId AS merchantId, t.first_payment AS firstPayment, t.second_payment AS secondPayment \n FROM Transactions \n MATCH_RECOGNIZE \n(   PARTITION BY merchantId  \n ORDER BY r_time  \n MEASURES\n     FIRST(amount) AS first_payment,\n     LAST(amount) AS second_payment\n   ONE ROW PER MATCH\n   AFTER MATCH SKIP PAST LAST ROW\n   PATTERN (A+ B)\n   DEFINE\n     A AS amount < 400,\n     B AS amount > 700 ) AS t',
    'WINDOW': 'SELECT column, COUNT(*) OVER (\n  PARTITION BY column\n  ORDER BY timestamp\n  RANGE INTERVAL \'5\' MINUTE PRECEDING\n)\nFROM Transactions'
  };

  if (!isOpen) return null;

  return (
    <div className="modal-overlay">
      <div className="modal-content" style={{ width: '35%' }}>
        <div className="modal-header">
          <h2>Add a new Rule</h2>
          <button className="close-btn" onClick={onClose}>×</button>
        </div>
        <div className="modal-body">
          <div className="sql-input-label">
            <span className="info-icon">i</span>
            <span>SQL</span>
          </div>
          <textarea
            value={sql}
            onChange={(e) => setSql(e.target.value)}
            rows="24"
            style={{ width: '36em' }}
            placeholder="Enter your SQL query here..."
          />
          <div className="template-buttons">
            {Object.entries(templates).map(([name, template]) => (
              <button
                key={name}
                className="template-btn"
                onClick={() => setSql(template)}
              >
                {name} Rule
              </button>
            ))}
          </div>
        </div>
        <div className="modal-footer">
          <button className="cancel-btn" onClick={onClose}>
            Cancel
          </button>
          <button 
            className="submit-btn"
            onClick={handleSubmit}
            disabled={!sql.trim()}
          >
            Submit
          </button>
        </div>
      </div>
    </div>
  );
}

export default AddRuleModal;