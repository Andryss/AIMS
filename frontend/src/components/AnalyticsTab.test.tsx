import React from 'react';
import { act, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, useLocation } from 'react-router-dom';
import * as api from '../api/client';
import { analyticsMeta, analyticsPath, analyticsPeriod, mockAnalyticsRequests } from '../test/analyticsTestData';
import { AnalyticsTab } from './AnalyticsTab';

jest.mock('../api/client');
const readPermissions = ['ANALYTICS_READ', 'INCIDENT_READ', 'EQUIPMENT_READ', 'DETENTION_READ', 'USER_READ', 'ALIEN_READ'];
function Location() { return <output aria-label="Адрес">{useLocation().search}</output>; }
function mount(path = analyticsPath(), token = 'tok', permissions = readPermissions) {
  return render(<MemoryRouter initialEntries={[path]}><Location /><AnalyticsTab token={token} permissions={permissions} /></MemoryRouter>);
}
beforeEach(() => { jest.clearAllMocks(); mockAnalyticsRequests(); });

it('loads each incident widget independently and preserves state when there are no period events', async () => {
  (api.getIncidentCreatedCount as jest.Mock).mockResolvedValue({ meta: analyticsMeta(), count: 0 });
  mount();
  expect(await within(screen.getByRole('region', { name: 'Всего инцидентов' })).findByText('3')).toBeInTheDocument();
  expect(within(screen.getByRole('region', { name: 'Зарегистрировано' })).getByText('0')).toBeInTheDocument();
  expect(screen.getByText('За период событий нет')).toBeInTheDocument();
  expect(api.getIncidentCreatedCount).toHaveBeenCalledWith('tok', analyticsPeriod);
  expect(api.getEquipmentReceivedCount).not.toHaveBeenCalled();
  expect(api.getDetentionCreatedCount).not.toHaveBeenCalled();
});

it('preserves the period and separate filters while switching dashboards', async () => {
  mount(analyticsPath() + '&incidents.eventType=ALIEN_CAPTURE');
  await waitFor(() => expect(api.getIncidentCreatedCount).toHaveBeenCalledWith('tok', {
    ...analyticsPeriod, eventType: 'ALIEN_CAPTURE',
  }));
  await userEvent.click(screen.getByRole('tab', { name: 'Склад' }));
  await waitFor(() => expect(api.getEquipmentReceivedCount).toHaveBeenCalledWith('tok', analyticsPeriod));
  fireEvent.change(screen.getByLabelText('Тип оборудования'), { target: { value: 'NEURALYZER' } });
  await waitFor(() => expect(api.getEquipmentReceivedCount).toHaveBeenLastCalledWith('tok', {
    ...analyticsPeriod, equipmentType: 'NEURALYZER',
  }));
  await userEvent.click(screen.getByRole('tab', { name: 'Инциденты' }));
  expect(screen.getByLabelText('Тип события')).toHaveValue('ALIEN_CAPTURE');
  expect(screen.getByLabelText('Адрес').textContent).toContain('equipment.equipmentType=NEURALYZER');
  await waitFor(() => expect(screen.queryAllByText(/^Загрузка:/)).toHaveLength(0));
});

it.each(['ATTACK_ON_PEOPLE', 'PROPERTY_DAMAGE', 'BIOLOGICAL_CONTAMINATION',
  'CRITICAL_INFRASTRUCTURE_DISRUPTION'])('requests analytics for the new event type %s', async (eventType) => {
  mount();
  await within(screen.getByRole('region', { name: 'Всего инцидентов' })).findByText('3');
  fireEvent.change(screen.getByLabelText('Тип события'), { target: { value: eventType } });
  expect(screen.getByLabelText('Тип события')).toHaveValue(eventType);
  await waitFor(() => expect(api.getIncidentCreatedCount).toHaveBeenLastCalledWith('tok', {
    ...analyticsPeriod, eventType,
  }));
});

it('debounces a valid period and does not request invalid or future dates', async () => {
  mount();
  await within(screen.getByRole('region', { name: 'Всего инцидентов' })).findByText('3');
  fireEvent.change(screen.getByLabelText('До'), { target: { value: '2099-01-01T12:00' } });
  expect(screen.getByRole('alert')).toHaveTextContent('будущем');
  expect(screen.queryByRole('region', { name: 'Всего инцидентов' })).not.toBeInTheDocument();
  await act(async () => { await new Promise((resolve) => setTimeout(resolve, 350)); });
  expect(api.getIncidentCreatedCount).toHaveBeenCalledTimes(1);
  fireEvent.change(screen.getByLabelText('До'), { target: { value: '2025-05-03T12:30' } });
  expect(api.getIncidentCreatedCount).toHaveBeenCalledTimes(1);
  await waitFor(() => expect(api.getIncidentCreatedCount).toHaveBeenLastCalledWith('tok', {
    from: analyticsPeriod.from, to: new Date('2025-05-03T12:30').toISOString(),
  }));
});

it.each(['bad', '2025-05-01T00:00:00Z', '2099-01-01T00:00:00Z',
  '2025-05-02T00:00:00', '2025-05-02T00:00:00.000000001Z'])('rejects invalid URL end %s', async (to) => {
  mount('/analytics?' + new URLSearchParams({ from: analyticsPeriod.from, to }));
  await act(async () => { await new Promise((resolve) => setTimeout(resolve, 350)); });
  expect(screen.getByRole('alert')).toBeInTheDocument();
  expect(api.getIncidentCreatedCount).not.toHaveBeenCalled();
});

it('keeps errors and retry local to a widget', async () => {
  (api.getIncidentTotalAtTo as jest.Mock).mockRejectedValueOnce(new Error('Нет связи'))
    .mockResolvedValue({ meta: analyticsMeta(), count: 18 });
  mount();
  expect(await screen.findByRole('alert')).toHaveTextContent('Нет связи');
  expect(within(screen.getByRole('region', { name: 'Зарегистрировано' })).getByText('3')).toBeInTheDocument();
  await userEvent.click(screen.getByRole('button', { name: 'Повторить: Всего инцидентов' }));
  expect(await within(screen.getByRole('region', { name: 'Всего инцидентов' })).findByText('18')).toBeInTheDocument();
  expect(api.getIncidentCreatedCount).toHaveBeenCalledTimes(1);
});

it('ignores a response from a previous period', async () => {
  let resolveOld!: (value: unknown) => void;
  (api.getIncidentTotalAtTo as jest.Mock).mockImplementationOnce(() => new Promise((resolve) => { resolveOld = resolve; }))
    .mockResolvedValue({ meta: analyticsMeta(), count: 71 });
  mount();
  fireEvent.change(screen.getByLabelText('До'), { target: { value: '2025-05-03T12:30' } });
  const region = await screen.findByRole('region', { name: 'Всего инцидентов' });
  expect(await within(region).findByText('71')).toBeInTheDocument();
  await act(async () => resolveOld({ meta: analyticsMeta(), count: 69 }));
  expect(screen.queryByText('69')).not.toBeInTheDocument();
});

it('renders employee avatars, historical fields and permission-aware links', async () => {
  (api.getIncidentStale as jest.Mock).mockResolvedValue({ meta: { ...analyticsMeta(), recordsWithoutSnapshot: 2 },
    totalCount: 12, items: [{ incidentId: 8, location: 'Area 51', status: 'EXECUTING',
      responsibleUserId: 3, lastChangedAt: '2025-04-01T00:00:00Z' }] });
  mount();
  expect(await screen.findByRole('link', { name: 'Инцидент #8' })).toHaveAttribute('href', '/incidents/8');
  expect(await screen.findByText('agent')).toBeInTheDocument();
  expect(screen.getByText(/Показаны первые 10 из 12/)).toBeInTheDocument();
  expect(screen.getByText(/Неполная история: 2/)).toBeInTheDocument();
});

it('does not fetch entity lookups or provide links without their read permissions', async () => {
  (api.getIncidentStale as jest.Mock).mockResolvedValue({ meta: analyticsMeta(), totalCount: 1,
    items: [{ incidentId: 8, location: 'Area 51', status: 'DRAFT', responsibleUserId: 3, lastChangedAt: analyticsPeriod.from }] });
  mount(analyticsPath(), 'tok', ['ANALYTICS_READ']);
  expect(await screen.findByText('Инцидент #8')).toBeInTheDocument();
  expect(screen.queryByRole('link', { name: 'Инцидент #8' })).not.toBeInTheDocument();
  expect(api.batchUsers).not.toHaveBeenCalled();
});

it('ignores a previous user response after the access token changes', async () => {
  let resolveOld!: (value: unknown) => void;
  (api.getIncidentTotalAtTo as jest.Mock).mockImplementationOnce(() => new Promise((resolve) => { resolveOld = resolve; }))
    .mockResolvedValue({ meta: analyticsMeta(), count: 31 });
  const view = render(<MemoryRouter initialEntries={[analyticsPath()]}>
    <AnalyticsTab token="old-user" permissions={readPermissions} /></MemoryRouter>);
  await waitFor(() => expect(api.getIncidentTotalAtTo).toHaveBeenCalledWith('old-user', analyticsPeriod));
  view.rerender(<MemoryRouter initialEntries={[analyticsPath()]}>
    <AnalyticsTab token="new-user" permissions={readPermissions} /></MemoryRouter>);
  expect(await within(screen.getByRole('region', { name: 'Всего инцидентов' })).findByText('31')).toBeInTheDocument();
  await act(async () => resolveOld({ meta: analyticsMeta(), count: 17 }));
  expect(screen.queryByText('17')).not.toBeInTheDocument();
});

it('supports keyboard navigation between dashboards', async () => {
  mount();
  const tab = screen.getByRole('tab', { name: 'Инциденты' });
  fireEvent.keyDown(tab, { key: 'ArrowRight' });
  expect(screen.getByRole('tab', { name: 'Склад' })).toHaveFocus();
  expect(screen.getByRole('tab', { name: 'Склад' })).toHaveAttribute('aria-selected', 'true');
  await waitFor(() => expect(screen.queryAllByText(/^Загрузка:/)).toHaveLength(0));
});

it.each([
  ['detentions', 'incidentId', 'Инцидент', '0'],
  ['detentions', 'incidentId', 'Инцидент', '-1'],
  ['detentions', 'incidentId', 'Инцидент', '1.5'],
  ['incidents', 'alienId', 'ID вида', '0'],
  ['incidents', 'responsibleUserId', 'ID ответственного', '1.5'],
])('does not broaden %s data for invalid %s=%s', async (dashboard, field, label, invalid) => {
  mount(analyticsPath(dashboard) + `&${dashboard}.${field}=8`, 'tok', ['ANALYTICS_READ']);
  await waitFor(() => expect(screen.queryAllByText(/^Загрузка:/)).toHaveLength(0));
  const load = dashboard === 'detentions' ? api.getDetentionCreatedCount : api.getIncidentCreatedCount;
  expect(load).toHaveBeenCalledTimes(1);
  fireEvent.change(screen.getByLabelText(label), { target: { value: invalid } });
  expect(screen.getByRole('alert')).toHaveTextContent('целый положительный');
  expect(screen.queryByRole('region', { name: dashboard === 'detentions' ? 'Создано заявок' : 'Зарегистрировано' })).not.toBeInTheDocument();
  await act(async () => { await new Promise((resolve) => setTimeout(resolve, 350)); });
  expect(load).toHaveBeenCalledTimes(1);
});
