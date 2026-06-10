import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import * as api from '../api/client';
import { mockIncident } from '../test/testData';
import { IncidentStatusSelect } from './IncidentStatusSelect';

jest.mock('../api/client');

const changeIncidentStatus = api.changeIncidentStatus as jest.MockedFunction<
  typeof api.changeIncidentStatus
>;

describe('IncidentStatusSelect', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('allows operator to move DRAFT to READY_FOR_ANALYSIS', async () => {
    const onStatusChanged = jest.fn();
    changeIncidentStatus.mockResolvedValue(
      mockIncident({ status: 'READY_FOR_ANALYSIS' }),
    );

    render(
      <IncidentStatusSelect
        token="tok"
        incident={mockIncident({ status: 'DRAFT' })}
        roles={['OPERATOR']}
        canChange
        onStatusChanged={onStatusChanged}
      />,
    );

    const select = screen.getByLabelText(/Статус инцидента #1/i);
    await userEvent.selectOptions(select, 'READY_FOR_ANALYSIS');

    await waitFor(() => {
      expect(changeIncidentStatus).toHaveBeenCalledWith('tok', 1, 'READY_FOR_ANALYSIS');
    });
    expect(onStatusChanged).toHaveBeenCalled();
  });

  it('disables select when user cannot change status', () => {
    render(
      <IncidentStatusSelect
        token="tok"
        incident={mockIncident({ status: 'DRAFT' })}
        roles={['ANALYST']}
        canChange
        onStatusChanged={jest.fn()}
      />,
    );

    expect(screen.getByLabelText(/Статус инцидента #1/i)).toBeDisabled();
  });
});
