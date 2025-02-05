package name.abuchen.portfolio.ui.views.dashboard.lists;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import name.abuchen.portfolio.model.Dashboard.Widget;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.model.SecurityEvent;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.UIConstants;
import name.abuchen.portfolio.ui.util.FormDataFactory;
import name.abuchen.portfolio.ui.util.LogoManager;
import name.abuchen.portfolio.ui.util.swt.StyledLabel;
import name.abuchen.portfolio.ui.views.dashboard.ChartHeightConfig;
import name.abuchen.portfolio.ui.views.dashboard.DashboardData;
import name.abuchen.portfolio.ui.views.dashboard.ReportingPeriodConfig;
import name.abuchen.portfolio.util.TextUtil;

public class EventListWidget extends AbstractSecurityListWidget<EventListWidget.EventItem>
{
    public static class EventItem extends AbstractSecurityListWidget.Item
    {
        private SecurityEvent event;

        public EventItem(Security security, SecurityEvent event)
        {
            super(security);
            this.event = event;
        }
    }

    public EventListWidget(Widget widget, DashboardData data)
    {
        super(widget, data);

        addConfig(new ReportingPeriodConfig(this));
        addConfig(new SortingConfig(this));
        addConfig(new ChartHeightConfig(this));
    }

    @Override
    public Supplier<List<EventItem>> getUpdateTask()
    {
        return () -> {

            var interval = get(ReportingPeriodConfig.class).getReportingPeriod().toInterval(LocalDate.now());

            List<EventItem> items = new ArrayList<>();
            for (Security security : getClient().getSecurities())
            {
                for (var event : security.getEvents())
                {
                    if (interval.contains(event.getDate()))
                    {
                        items.add(new EventItem(security, event));
                    }
                }
            }

            var comparator = get(SortingConfig.class).getValue().getComparator();
            Collections.sort(items, (r, l) -> comparator.compare(r.event.getDate(), l.event.getDate()));

            return items;
        };
    }

    @Override
    protected Composite createItemControl(Composite parent, EventItem item)
    {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new FormLayout());

        String type = item.event.getType().toString();
        String details = null;

        switch (item.event.getType())
        {
            case NOTE:
                details = item.event.getDetails();
                break;
            case STOCK_SPLIT:
                type += " " + item.event.getDetails(); //$NON-NLS-1$
                break;
            case DIVIDEND_PAYMENT:
                details = Values.Money.format(item.event instanceof SecurityEvent.DividendEvent dividendPayment
                                ? dividendPayment.getAmount()
                                : null);
                break;
            default:
        }

        Label logo = createLabel(composite,
                        LogoManager.instance().getDefaultColumnImage(item.getSecurity(), getClient().getSettings()));

        Label lblType = createLabel(composite, Values.Date.format(item.event.getDate()) + ": " + type); //$NON-NLS-1$
        lblType.setData(UIConstants.CSS.CLASS_NAME, UIConstants.CSS.HEADING2);
        Label name = createLabel(composite, item.getSecurity().getName());

        composite.addMouseListener(mouseUpAdapter);
        name.addMouseListener(mouseUpAdapter);
        lblType.addMouseListener(mouseUpAdapter);

        FormDataFactory.startingWith(lblType).thenBelow(logo).thenRight(name);

        if (details != null)
        {
            Label lblDetails = new Label(composite, SWT.WRAP);
            lblDetails.setText(TextUtil.tooltip(details));
            lblDetails.addMouseListener(mouseUpAdapter);
            FormDataFactory.startingWith(name).thenBelow(lblDetails).width(200).right(new FormAttachment(100));
        }

        return composite;
    }

    @Override
    protected void createEmptyControl(Composite parent)
    {
        title = new StyledLabel(parent, SWT.WRAP);
        title.setText(Messages.MsgHintNoEvents);
    }

}
