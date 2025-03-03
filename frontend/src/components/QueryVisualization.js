import React from 'react';

const QueryVisualization = ({ data }) => {

  return (
    <div className="query-visualization">
      {data && data.length > 0 ? (
        <table>
          <thead>
            <tr>
              {data[0].fieldNames.map((field, index) => (
                <th key={index}>{field}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {data.map((item, rowIndex) => (
              <tr key={rowIndex}>
                {item.data.map((value, colIndex) => (
                  <td key={colIndex}>{value}</td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      ) : (
        <p>No data available.</p>
      )}
    </div>
  );
};

export default QueryVisualization;