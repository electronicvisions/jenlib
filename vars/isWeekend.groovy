/**
 * Find out if today is a weekend's day.
 *
 * @return {@code true} if today is Saturday or Sunday.
 */
static boolean call() {
	Date date = new Date()
	Calendar calendar = Calendar.getInstance()
	calendar.setTime(date)
	int day = calendar.get(Calendar.DAY_OF_WEEK)
	return (day == Calendar.SATURDAY) || (day == Calendar.SUNDAY)
}
